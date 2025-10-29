# Implementación del Operador LIMIT

## Descripción

Se ha implementado exitosamente el operador LIMIT para la clase QuerySelect, permitiendo limitar el número de filas devueltas en una consulta SELECT.

## Características implementadas

1. **Clase Limit**: Se creó una nueva clase `Limit` en el paquete `operator` que maneja los parámetros de límite y offset.

2. **Soporte en QuerySelect**: Se agregó soporte completo para LIMIT en la clase QuerySelect y su QueryBuilder.

3. **Métodos disponibles**:
   - `limit(count: Int)`: Limita el número de filas a devolver
   - `limit(count: Int, offset: Int?)`: Limita el número de filas con un offset opcional
   - `limit(limitOperator: Limit)`: Acepta un objeto Limit directamente

## Ejemplos de uso

### Uso básico con Builder
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .limit(10)
    .build()

// Genera: SELECT * FROM users WHERE status = 'active' LIMIT 10
```

### Uso con offset
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .limit(10, 5)
    .build()

// Genera: SELECT * FROM users WHERE status = 'active' LIMIT 10 OFFSET 5
```

### Uso con objeto Limit
```kotlin
val limitOperator = Limit(count = 10, offset = 5)
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .limit(limitOperator)
    .build()

// Genera: SELECT * FROM users WHERE status = 'active' LIMIT 10 OFFSET 5
```

### Encadenamiento de métodos
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .and("age", SQLOperator.GreaterThan("age", 18))
    .orderBy(OrderBy.Desc("created_at"))
    .limit(20)
    .build()

// Genera: SELECT * FROM users WHERE status = 'active' AND age > 18
//         ORDER BY created_at DESC
//         LIMIT 20
```

### Modificación de límite en consulta existente
```kotlin
val query = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .build()

// Agregar límite después de crear la consulta
query.limit(15, 10)

// Genera: SELECT * FROM users WHERE status = 'active' LIMIT 15 OFFSET 10
```

## Validaciones implementadas

La clase Limit incluye validaciones para garantizar que:
- El count debe ser no negativo
- El offset (si se proporciona) debe ser no negativo

## Orden de ejecución SQL

La implementación respeta el orden estándar de SQL:
1. SELECT campos
2. FROM tabla
3. WHERE condiciones
4. ORDER BY (si existe)
5. LIMIT (si existe)

Esta implementación permite crear consultas SQL complejas con paginación de manera fluida y manteniendo la consistencia con el resto del framework.
