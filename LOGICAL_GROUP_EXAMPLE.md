# Implementación de Agrupación de Condiciones AND / OR

## Descripción

Se ha implementado la agrupación de condiciones para las clases `QuerySelect` y `QueryDelete`,
permitiendo emitir grupos parentizados `AND (...)` y `OR (...)` dentro de una cláusula `WHERE`.

El problema que resuelve la feature es de precedencia. Las condiciones se encadenaban en un único
nivel con `joinToString(" ")`, de modo que un `OR` agregado al final de una cadena de `AND` se
interpretaba como `(a AND b) OR (...)` en lugar de `a AND b AND (...)`. La biblioteca no contaba con
ninguna API para expressing esa intención.

## Características implementadas

1. **`LogicalOperation.Multiple` corregido** (`operator/LogicalOperation.kt`):
   - `asString()` ahora usa `asConditionString()` —que delega en `SQLOperator.toSQLString()`— para
     la primera condición del grupo, por lo que los valores `String` conservan sus comillas. Antes
     usaba `asString()`, que las eliminaba.
   - La primera condición ya no pierde su conector cuando el grupo se anida.
   - `operations` no puede ser una lista vacía; el constructor lanza `IllegalArgumentException`.
   - `symbol` solo admite `AND` u `OR`. Cualquier otro valor lanza `IllegalArgumentException` en
     lugar de emitir un conector arbitrario.
   - Nuevo constructor `vararg`: `Multiple(Or(a), Or(b))`.
   - `operators()` y `sqlOperators()` recorren los grupos anidados y devuelven todos los operadores
     en profundidad.

2. **Renderizado de la posición `WHERE`**: una operación puede.renderizarse sin su conector para
   ocupar el ancla de la cláusula. Un grupo en esa posición emite `WHERE (a OR b)` en lugar del
   inválido `WHERE AND (a OR b)`.

3. **Filtering recursivo** (`filterBy`): `asSql(predicate)` entra dentro de los grupos y conserva
   las condiciones que el predicado acepta, en lugar de descartar el grupo completo por una sola
   condición. Si el `WHERE` no sobrevive al filtro, la primera condición restante asciende a la
   posición de `WHERE`, de modo que la sentencia nunca empieza con un conector colgante.

4. **Builders con grupos** (`QuerySelect.QueryBuilder` y `QueryDelete.QueryBuilder`):
   - `andGroup(key, vararg conditions)`: adjunta el grupo a la cadena con `AND`.
   - `orGroup(key, vararg conditions)`: adjunta el grupo a la cadena con `OR`.
   - `andGroup(key, connector, vararg conditions)` y `orGroup(key, connector, vararg conditions)`:
     atajo que aplica [connector] entre las condiciones internas sin envolver cada una en
     `LogicalOperation`.

5. **`andGroup()` / `orGroup()` en las instancias** de `QuerySelect` y `QueryDelete`, con las mismas
   cuatro sobrecargas, para agregar un grupo sobre una query ya construida sin pasar por
   `newBuilder`.

6. **`QueryDelete.QueryBuilder.addLogicalOperation(key, operation)`**: se agregó para mantener
   paridad con `QuerySelect.QueryBuilder` y con la instancia de `QueryDelete`, que ya lo exponía.

7. **`copyOperation` sobre grupos** (`utils/Extensions.kt`): reemplaza el operador de la primera
   condición **no agrupada**, descendiendo en los grupos anidados. Reemplazar el primer elemento
   anidaba un grupo dentro de otro y emitía un conector inválido como `AND (OR (a OR b))`.

8. **`getSqlOperators()` y `getOperators()`** de `QuerySelect` y `QueryDelete` devuelven todos los
   operadores de los grupos, no solo el primero, de modo que la serialización hacia
   `RepeatedQueryParameters` ya no pierde condiciones. `getOperations()` sigue devolviendo un
   operador representativo por clave, porque su valor es un `SQLOperator` y no admite listas.

## Semántica de los símbolos

El símbolo del grupo define **cómo se conecta** el grupo a la cláusula que lo contiene. El símbolo de
cada condición define **cómo se combina con la anterior dentro** del grupo. Esa separación es la que
permite expresar `AND (a OR b OR c)` y `OR (a AND b AND c)` con la misma primitiva.

## Ejemplos de uso

### Búsqueda con varios campos y precedencia AND / OR
```kotlin
val query = QuerySelect.builder("tire_transfer_document")
    .where(SQLOperator.Equals("creator", 1))
    .and("status", SQLOperator.Equals("status", 1))
    .and("responsible", SQLOperator.NotEquals("responsible", 1))
    .andGroup(
        "search",
        LogicalType.OR,
        SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER),
        SQLOperator.Like("terminal_source_display", "cal", CaseConversion.LOWER),
        SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER)
    )
    .build()

// Genera:
// SELECT * FROM tire_transfer_document WHERE creator = 1 AND status = 1 AND responsible != 1
//   AND (LOWER(doc_number) LIKE LOWER('%cal%')
//        OR LOWER(terminal_source_display) LIKE LOWER('%cal%')
//        OR LOWER(terminal_dest_display) LIKE LOWER('%cal%'))
```

Sin el grupo, la versión con `or()` produciría `... AND responsible != 1 OR (...)`, que SQL resuelve
como `(creator = 1 AND status = 1 AND responsible != 1) OR (...)`.

### Grupo con conectores internos mixtos
Cuando las condiciones internas no comparten conector, se pasan como `LogicalOperation`:
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("id", 1))
    .andGroup(
        "combo",
        LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
        LogicalOperation.And(SQLOperator.GreaterThan("age", 18))
    )
    .build()

// SELECT * FROM users WHERE id = 1 AND (role = 'admin' AND age > 18)
```

### Grupo conectado con OR
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .orGroup(
        "role",
        LogicalOperation.And(SQLOperator.Equals("role", "admin")),
        LogicalOperation.And(SQLOperator.Equals("role", "root"))
    )
    .build()

// Genera: SELECT * FROM users WHERE status = 'active' OR (role = 'admin' AND role = 'root')
```

### Grupo conectado con AND
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("id", 1))
    .andGroup(
        "range",
        LogicalOperation.And(SQLOperator.GreaterThan("age", 18)),
        LogicalOperation.And(SQLOperator.LessThan("age", 65))
    )
    .build()

// Genera: SELECT * FROM users WHERE id = 1 AND (age > 18 AND age < 65)
```

### Grupos anidados
```kotlin
val group = LogicalOperation.Multiple(
    listOf(
        LogicalOperation.And(SQLOperator.Equals("status", "active")),
        LogicalOperation.Multiple(
            listOf(
                LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
                LogicalOperation.Or(SQLOperator.Equals("role", "root"))
            ),
            LogicalType.OR.sql
        )
    ),
    LogicalType.AND.sql
)

// Genera: AND (status = 'active' OR (role = 'admin' OR role = 'root'))
```

### DELETE con grupo
```kotlin
val query = QueryDelete.builder("sessions")
    .where(SQLOperator.LessThan("expires_at", now))
    .andGroup(
        "stale",
        LogicalType.OR,
        SQLOperator.IsNull("refreshed_at"),
        SQLOperator.LessThan("refreshed_at", threshold)
    )
    .build()

// Genera:
// DELETE FROM sessions WHERE expires_at < 1700000000
//   AND (refreshed_at IS NULL OR refreshed_at < 1600000000)
```

### Grupo como ancla de WHERE
```kotlin
val query = QuerySelect.builder("users")
    .where(
        "search",
        LogicalOperation.Multiple(
            LogicalOperation.Or(SQLOperator.Equals("role", "admin")),
            LogicalOperation.Or(SQLOperator.Equals("role", "root"))
        )
    )
    .and("status", SQLOperator.Equals("status", "active"))
    .build()

// Genera: SELECT * FROM users WHERE (role = 'admin' OR role = 'root') AND status = 'active'
```

### Grupo agregado sobre una query ya construida
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .build()

query.andGroup("search", LogicalType.OR, SQLOperator.Equals("role", "admin"))

// Genera: SELECT * FROM users WHERE status = 'active' AND (role = 'admin')
```

### Filtering dentro del grupo
```kotlin
val query = QuerySelect.builder("tire_transfer_document")
    .where(SQLOperator.Equals("creator", 1))
    .andGroup(
        "search",
        LogicalType.OR,
        SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER),
        SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER)
    )
    .build()

query.asSql { it.column != "terminal_dest_display" }
// SELECT * FROM tire_transfer_document WHERE creator = 1
//   AND (LOWER(doc_number) LIKE LOWER('%cal%'))
```

### Inspección del contenido de un grupo
```kotlin
val group = LogicalOperation.Multiple(
    LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")),
    LogicalOperation.Or(SQLOperator.Equals("terminal_dest_display", "cal"))
)

group.operators().map { it.toPair() }
// [(doc_number, cal), (terminal_dest_display, cal)]
```

## Validaciones implementadas

- `Multiple` requiere al menos una operación; una lista vacía lanza `IllegalArgumentException` en el
  momento de la construcción y no en el momento de generar el SQL.
- `Multiple` solo admite `AND` u `OR` como conector. `WHERE`, `EXISTS` o cualquier cadena arbitraria
  lanzan `IllegalArgumentException` en lugar de emitir SQL inválido.
- `andGroup` y `orGroup` delegan esas validaciones: llamar `andGroup("search")` sin condiciones lanza
  `IllegalArgumentException`.
- El grupo se registra en el mismo `LinkedHashMap<String, LogicalOperation>` que el resto de las
  condiciones, por lo que `remove(key)`, la sobreescritura por clave duplicada y el orden de emisión
  funcionan igual que con una condición simple.

## Tests

`query/src/test/java/com/blipblipcode/query/LogicalGroupTest.kt` cubre:

- El caso de búsqueda de múltiples campos con precedencia AND / OR.
- La conexión del grupo con `AND` y con `OR`, y el atajo con conector interno tipado.
- El renderizado de valores `String` con comillas dentro del grupo.
- El anidamiento de grupos y el recorrido de sus operadores.
- El filtrado por `predicate` dentro del grupo, la desaparición del grupo vacío y la promoción de
  la primera condición superviviente cuando el `WHERE` no pasa el filtro, en SELECT y en DELETE.
- El grupo usado como ancla de `WHERE`, renderizado sin conector colgante.
- La inspección del contenido con `operators()` y con `getSqlOperators()`.
- Los `IllegalArgumentException` por grupo vacío y por conector inválido.
- La invariancia del renderizado plano cuando no hay grupos.
- El clonado profundo al copiar la consulta y el reemplazo de la primera hoja en `copyOperation`.
- La eliminación de un grupo por clave con `remove(key)`.
- El agregado de grupos sobre una instancia ya construida.

## Orden de ejecución SQL

La implementación no altera el orden de las cláusulas:

1. SELECT campos
2. FROM tabla
3. WHERE condición principal + grupos y condiciones
4. ORDER BY (si existe)
5. LIMIT (si existe)

## Compatibilidad

No hay cambios de comportamiento para las consultas existentes. El renderizado de una cadena de
condiciones sin grupos sigue siendo plano, exactamente igual que antes, por lo que ninguna consulta
generada previamente cambia de string. `LogicalOperation.Multiple` no tenía cobertura de tests ni
call sites de producción antes de esta feature, y su renderizado anterior emitía SQL inválido
(valores `String` sin comillas), de modo que corregirlo no puede afectar a un consumidor que
dependiera de ese output.

Los dos comportamientos corregidos que sí cambian el resultado, ambos casos de SQL inválido o pérdida
silenciosa de condiciones, solo eran alcanzables a través de `Multiple` o del overload de `predicate`:

- `asSql(predicate)` ya no descarta un grupo entero por una sola condición rechazada, y ya no deja
  una sentencia empezando por un conector colgante cuando el `WHERE` es filtrado.
- `getSqlOperators()` ya no expone un solo operador por grupo, lo que afecta a
  `asQueryRepeatedQueryParameters()` de las consultas con grupos.