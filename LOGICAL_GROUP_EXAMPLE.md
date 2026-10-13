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
   - `asString()` ahora usa `SQLOperator.toSQLString()` para la primera condición del grupo, por lo
     que los valores `String` conservan sus comillas. Antes usaba `asString()`, que las eliminaba.
   - La primera condición ya no pierde su conector cuando el grupo se anida: un `Multiple` en la
     posición 0 emite su propio símbolo.
   - `operations` no puede ser una lista vacía; el constructor lanza `IllegalArgumentException`.
   - Nuevo `operators()`: devuelve todos los operadores del grupo en orden de declaración.

2. **Builders con grupos** (`QuerySelect.QueryBuilder` y `QueryDelete.QueryBuilder`):
   - `andGroup(key, vararg conditions)`: adjunta el grupo a la cadena con `AND`.
   - `orGroup(key, vararg conditions)`: adjunta el grupo a la cadena con `OR`.

3. **`QueryDelete.QueryBuilder.addLogicalOperation(key, operation)`**: se agregó para mantener
   paridad con `QuerySelect.QueryBuilder` y con la instancia de `QueryDelete`, que ya lo exponía.

4. **`Multiple.operators()`** expone el contenido completo del grupo para inspección o mutación,
   mientras que `operator`, `getSqlOperators()` y `getOperations()` mantienen su comportamiento
   previo (devuelven el primer operador) para no romper la compatibilidad.

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
        LogicalOperation.Or(SQLOperator.Like("doc_number", "cal", CaseConversion.LOWER)),
        LogicalOperation.Or(SQLOperator.Like("terminal_source_display", "cal", CaseConversion.LOWER)),
        LogicalOperation.Or(SQLOperator.Like("terminal_dest_display", "cal", CaseConversion.LOWER))
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
        LogicalOperation.Or(SQLOperator.IsNull("refreshed_at")),
        LogicalOperation.Or(SQLOperator.LessThan("refreshed_at", threshold))
    )
    .build()

// Genera:
// DELETE FROM sessions WHERE expires_at < 1700000000
//   AND (refreshed_at IS NULL OR refreshed_at < 1600000000)
```

### Inspección del contenido de un grupo
```kotlin
val group = LogicalOperation.Multiple(
    listOf(
        LogicalOperation.Or(SQLOperator.Equals("doc_number", "cal")),
        LogicalOperation.Or(SQLOperator.Equals("terminal_dest_display", "cal"))
    ),
    LogicalType.AND.sql
)

group.operators().map { it.toPair() }
// [(doc_number, cal), (terminal_dest_display, cal)]
```

## Validaciones implementadas

- `Multiple` requiere al menos una operación; una lista vacía lanza `IllegalArgumentException` en el
  momento de la construcción y no en el momento de generar el SQL.
- `andGroup` y `orGroup` delegan esa validación: llamar `andGroup("search")` sin condiciones lanza
  `IllegalArgumentException`.
- El grupo se registra en el mismo `LinkedHashMap<String, LogicalOperation>` que el resto de las
  condiciones, por lo que `remove(key)`, la sobreescritura por clave duplicada y el orden de emisión
  funcionan igual que con una condición simple.

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
(valores `String` sin comillas), de modo que corregirlo no puede affectar a un consumidor que
dependiera de ese output.

## Tests

`query/src/test/java/com/blipblipcode/query/LogicalGroupTest.kt` cubre:

- El caso de búsqueda de múltiples campos con precedencia AND / OR.
- La conexión del grupo con `AND` y con `OR`.
- El renderizado de valores `String` con comillas dentro del grupo.
- El anidamiento de grupos.
- La inspección del contenido con `operators()`.
- El `IllegalArgumentException` ante un grupo vacío.
- La invariancia del renderizado plano cuando no hay grupos.
- El comportamiento equivalente en `QueryDelete`.
- El filtrado por `predicate` en `asSql(predicate)`.
- El clonado profundo al copiar la consulta.
- La eliminación de un grupo por clave con `remove(key)`.