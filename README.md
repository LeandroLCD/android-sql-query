<div align="center">

# 🗄️ Android SQL Query Builder

### Una librería de Kotlin ligera y fluida para construir consultas SQL de forma programática

[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21+-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2021+-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Room](https://img.shields.io/badge/Room-Compatible-blue.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![Retrofit](https://img.shields.io/badge/Retrofit-Compatible-red.svg?style=flat)]([https://developer.android.com/training/data-storage/room](https://square.github.io/retrofit/))
![GitHub release (latest by date)](https://img.shields.io/github/v/release/LeandroLCD/android-sql-query)
[![Run Query Unit Tests](https://github.com/LeandroLCD/android-sql-query/actions/workflows/run-query-tests.yml/badge.svg)](https://github.com/LeandroLCD/android-sql-query/actions/workflows/run-query-tests.yml)


Diseñada para integrarse perfectamente con la base de datos de Android y Room a través de `@RawQuery`, permitiendo la creación de consultas dinámicas de manera segura y legible.

[Características](#-características) •
[Instalación](#-instalación) •
[Uso](#-uso) •
[Integración con Room](#-integración-con-room) •
[RepeatedQueryParameters](#-repeatedqueryparameters) •
[Contribuir](#-contribuir)

</div>

---

## 📋 Tabla de Contenidos

- [✨ Características](#-características)
- [📦 Instalación](#-instalación)
- [🚀 Uso](#-uso)
  - [SELECT](#-select)
  - [INSERT](#-insert)
  - [UPDATE](#-update)
  - [DELETE](#-delete)
  - [INNER JOIN](#-inner-join)
- [🔗 Integración con Room](#-integración-con-room)
- [📡 Integración con retrofit -> RepeatedQueryParameters](#-repeatedqueryparameters)
- [📝 Ejemplos Avanzados](#-ejemplos-avanzados)
- [🤝 Contribuir](#-contribuir)

---

## ✨ Características

- 🔗 **Constructor de Consultas Fluido**: API encadenable para una construcción de consultas clara y concisa
- 📊 **Soporte Completo de Operaciones**: `SELECT`, `INSERT`, `UPDATE`, y `DELETE`
- 🔄 **Joins Complejos**: Soporte para `INNER JOIN`
- ⚡ **Operadores SQL**: Amplia gama de operadores (`=`, `!=`, `>`, `<`, `LIKE`, `IN`, `BETWEEN`, `IS NULL`, etc.)
- 🧩 **Condiciones Lógicas**: Combina cláusulas fácilmente con `AND` y `OR`
- 🏛️ **Integración con Room**: Conversión directa a `SupportSQLiteQuery` con la función de extensión `asSQLiteQuery()`
- 💯 **100% Kotlin**: Código moderno, idiomático y nulo-seguro

---

## 📦 Instalación

Agrega la dependencia a tu archivo `build.gradle`:


1. Agrega el repositorio de JitPack en tu archivo `build.gradle` a nivel de proyecto:

```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

2. Luego, en el archivo `build.gradle` de tu módulo, agrega la dependencia:

```gradle
dependencies {
    implementation 'com.github.blipblipcode:android-sql-query:0.0.4'
}
```
---

## 🚀 Uso

A continuación se muestran ejemplos de cómo construir los diferentes tipos de consultas.

### 🔍 SELECT

Para construir una consulta `SELECT`, utiliza `QuerySelect.builder()`.

```kotlin
// Construye: SELECT name, email FROM users WHERE age > 30 AND status = 'active'
val selectQuery = QuerySelect.builder("users")
    .where(SQLOperator.GreaterThan("age", 30))
    .and("status", SQLOperator.Equals("status", "active"))
    .setFields("name", "email")
    .build()
// Agrega un límite de 10 resultados
    selectQuery.limit(10)
// Agrega un offset 
    selectQuery.limit(10, 5) 

val sqlString = selectQuery.asSql()

### ⬆️ ORDER BY

Para ordenar los resultados puedes utilizar el método `orderBy` con `OrderBy.Asc`, `OrderBy.Desc` o una lista mediante `OrderBy.Multiple`.

`OrderBy.Asc` y `OrderBy.Desc` aceptan tres parámetros:

| Parámetro   | Tipo                   | Descripción |
|-------------|------------------------|-------------|
| `column`    | `String`               | Nombre de la columna. |
| `collation` | `Collation`            | Cláusula COLLATE que se añade **al final** de la expresión (por defecto `Collation.NONE`). |
| `transform` | `(String) -> String`   | Función que envuelve la columna en SQL arbitrario (REPLACE, LOWER, etc.). Se aplica **antes** del COLLATE. Por defecto es la identidad `{ it }`. |

```kotlin
// ORDER BY simple asc
val q1 = QuerySelect.builder("users")
    .where(SQLOperator.Equals("status", "active"))
    .build()
q1.orderBy(OrderBy.Asc("name")) // -> ORDER BY name ASC

// ORDER BY con múltiples columnas y direcciones mezcladas
q1.orderBy(OrderBy.Multiple(listOf(
    OrderBy.Asc("department"),
    OrderBy.Desc("created_at")
))) // -> ORDER BY department ASC, created_at DESC

// ORDER BY usando Collation (ej: NOCASE o RTRIM)
q1.orderBy(OrderBy.Asc("name", collation = Collation.NOCASE))
// -> ORDER BY name COLLATE NOCASE ASC

q1.orderBy(OrderBy.Desc("name", collation = Collation.RTRIM))
// -> ORDER BY RTRIM(name) DESC
```

#### 🔄 Transform + Collation

Usa `transform` para aplicar funciones SQL sobre la columna (como `REPLACE`) y `collation` para
el `COLLATE`. El `transform` envuelve la columna **antes** de que se añada el COLLATE,
garantizando SQL correcto como:

```sql
SELECT * FROM vehicle
ORDER BY REPLACE(identification, '-', '') COLLATE NOCASE ASC
```

> **Nota:** En SQLite, `LOWER()` no garantiza un ordenamiento case-insensitive correcto.
> Usa siempre `COLLATE NOCASE` para ignorar mayúsculas/minúsculas al ordenar.

```kotlin
// Eliminar guiones e ignorar mayúsculas/minúsculas
val strip = "-".removeCharsTransform()   // extensión de CollationUtils
q1.orderBy(OrderBy.Asc("identification", collation = Collation.NOCASE, transform = strip))
// -> ORDER BY REPLACE(identification, '-', '') COLLATE NOCASE ASC

// Solo COLLATE NOCASE (sin transform)
q1.orderBy(OrderBy.Asc("name", collation = Collation.NOCASE))
// -> ORDER BY name COLLATE NOCASE ASC

// Quitar espacios, guiones y puntos + COLLATE NOCASE
val stripChars = " -.".removeCharsTransform()
q1.orderBy(OrderBy.Asc("name", collation = Collation.NOCASE, transform = stripChars))
// -> ORDER BY REPLACE(REPLACE(REPLACE(name, ' ', ''), '-', ''), '.', '') COLLATE NOCASE ASC

// Combinar RTRIM + NOCASE con transform
val stripDash = "-".removeCharsTransform()
q1.orderBy(OrderBy.Asc("code", collation = Collation.RTRIM and Collation.NOCASE, transform = stripDash))
// -> ORDER BY RTRIM(REPLACE(code, '-', '')) COLLATE NOCASE ASC
```

### ➕ INSERT

Para construir una consulta `INSERT`, utiliza `QueryInsert.builder()`.

```kotlin
// Construye: INSERT INTO users (name, age) VALUES ('Jane Doe', 28)
val insertQuery = QueryInsert.builder("users")
    .add("name", "Jane Doe")
    .add("age", 28)
    .build()

val sqlString = insertQuery.asSql()
```

### 🔄 UPDATE

Para construir una consulta `UPDATE`, utiliza `QueryUpdate.builder()`.

```kotlin
// Construye: UPDATE users SET status = 'inactive' WHERE name = 'Jane Doe'
val updateQuery = QueryUpdate.builder("users")
    .set("status", "inactive")
    .where(SQLOperator.Equals("name", "Jane Doe"))
    .build()

val sqlString = updateQuery.asSql()
```

### 🗑️ DELETE

Para construir una consulta `DELETE`, utiliza `QueryDelete.builder()`.

```kotlin
// Construye: DELETE FROM users WHERE status = 'inactive'
val deleteQuery = QueryDelete.builder("users")
    .where(SQLOperator.Equals("status", "inactive"))
    .build()

val sqlString = deleteQuery.asSql()
```

### 🔗 INNER JOIN

La librería permite encadenar múltiples `INNER JOIN` de forma sencilla.

```kotlin
// Construye: (SELECT * FROM orders) INNER JOIN (SELECT id, name FROM customers) ON orders.customer_id = customers.id

val queryOrders = QuerySelect.builder("orders").where(SQLOperator.Equals("id", 1)).build()
val queryCustomers = QuerySelect.builder("customers").where(SQLOperator.Equals("id", 1)).setFields("id", "name").build()

val joinQuery = queryOrders.innerJoin(queryCustomers, "orders.customer_id = customers.id")

val sqlString = joinQuery.asSql()
```

---

## 🏛️ Integración con Room

La principal ventaja de esta librería es su capacidad para generar consultas dinámicas para Room de forma segura. Se integra perfectamente con métodos DAO anotados con `@RawQuery`.

### 1️⃣ Define tu Entidad

```kotlin
@Entity
data class User(
    @PrimaryKey val id: Int,
    val name: String,
    val age: Int,
    val status: String
)
```

### 2️⃣ Crea un Método DAO con @RawQuery

El método debe aceptar un objeto `SupportSQLiteQuery`.

```kotlin
@Dao
interface UserDao {
    @RawQuery
    fun getUsers(query: SupportSQLiteQuery): List<User>
}
```

### 3️⃣ Construye y Ejecuta tu Consulta Dinámica

Desde tu repositorio o ViewModel, construye la consulta, conviértela con `asSQLiteQuery()` y pásala al método del DAO.

```kotlin
class UserRepository(private val userDao: UserDao) {

    fun findActiveUsers(minAge: Int, nameFilter: String): List<User> {
        // Construye la consulta dinámica
        val query = QuerySelect.builder("User") // Room usa el nombre de la clase o @Entity(tableName)
            .where(SQLOperator.GreaterThan("age", minAge))
            .and("status", SQLOperator.Equals("status", "active"))
            .and("name", SQLOperator.Like("name", nameFilter))
            .setFields("id", "name", "age", "status")
            .build()

        // Convierte a SupportSQLiteQuery y ejecuta
        return userDao.getUsers(query.asSQLiteQuery())
    }
}
```

---

## 📡 RepeatedQueryParameters

`RepeatedQueryParameters` es una clase utilitaria destinada a facilitar el uso de parámetros de consulta repetidos cuando se integra con Retrofit y otras librerías que consumen `Map`/`QueryMap` de parámetros.

Descripción breve:
- Permite pasar listas como valores en un `@QueryMap` y que Retrofit las expanda como múltiples pares clave=valor en la URL (ej: `?tag=a&tag=b`).
- Mantiene el orden de inserción (hereda de `LinkedHashMap`) para reproducibilidad en pruebas y cachés.
- Omite elementos `null` dentro de listas y lanza excepción si se intenta usar una clave o valor `null`.

API y métodos principales:
- `RepeatedQueryParameters.create(vararg pairs: Pair<String, Any>): RepeatedQueryParameters` — Crea la instancia a partir de pares clave/valor.
- `RepeatedQueryParameters.fromMap(map: MutableMap<String, Any>): RepeatedQueryParameters` — Convierte un `Map` existente.
- `RepeatedQueryParameters.empty(): RepeatedQueryParameters` — Instancia vacía.
- `addParameter(key: String, value: Any)` — Agrega o reemplaza un parámetro simple.
- `addRepeatedParameter(key: String, values: List<*>)` — Agrega una lista que será expandida.

Compatibilidad con la extensión `Queryable.asQueryRepeatedQueryParameters`:

La librería expone una extensión `Queryable.asQueryRepeatedQueryParameters()` que convierte los operadores SQL (devueltos por `getSqlOperators()`) en una instancia de `RepeatedQueryParameters`. Esta extensión:
- Filtra los operadores usando un `predicate: (Pair<String, Any?>) -> Boolean` opcional.
- Expande automáticamente listas en `RepeatedQueryParameters` mediante `addRepeatedParameter`.

Ejemplo usando la extensión `asQueryRepeatedQueryParameters`:

```kotlin
// Supongamos que `query` es un QuerySelect u otro Queryable con operadores que incluyen listas
val params = query.asQueryRepeatedQueryParameters()
// ahora `params` puede ser pasado directamente a Retrofit como @QueryMap
```

Ejemplo de uso con Retrofit:

```kotlin
interface ProductApi {
    @GET("api/v2/product/list")
    suspend fun getProductList(
        @QueryMap options: RepeatedQueryParameters
    ): ResponsePaginListDto<ProductItemDto>
}

// Construcción de parámetros
val options = RepeatedQueryParameters.create(
    "limit" to 50,
    "offset" to 0,
    "status" to listOf("active", "pending"),
    "brand" to "michelin",
    "sort" to "date"
)

// Llamada al API
val response = api.getProductList(options = options)
// Resultado en URL: ?limit=50&offset=0&status=active&status=pending&brand=michelin&sort=date
```

Notas de uso y buenas prácticas:
- Evita pasar valores `null` como clave o valor (la clase lanza excepción).
- Para agregar dinámicamente parámetros desde un `Queryable`, usa `asQueryRepeatedQueryParameters()` y opcionalmente provee un `predicate` para incluir/excluir pares.
- Mantén simples los valores no list (String, Int, Boolean); las listas son las que generan repetición en la URL.

---

## 📝 Ejemplos Avanzados

### Consultas Complejas con Múltiples Condiciones

```kotlin
val complexQuery = QuerySelect.builder("products")
    .where(SQLOperator.GreaterThan("price", 100))
    .and("category", SQLOperator.In("category", listOf("Electronics", "Computers")))
    .and("stock", SQLOperator.Between("stock", 10, 100))
    .or("featured", SQLOperator.Equals("featured", true))
    .setFields("id", "name", "price")
    .build()
```

### Actualización Masiva

```kotlin
val bulkUpdate = QueryUpdate.builder("inventory")
    .set("discount", 0.15)
    .set("updated_at", System.currentTimeMillis())
    .where(SQLOperator.LessThan("stock", 5))
    .build()
```

---

## 🤝 Contribuir

¡Las contribuciones son bienvenidas! Si deseas contribuir:

1. 🍴 Haz un Fork del proyecto
2. 🌿 Crea una rama para tu función (`git checkout -b feature/AmazingFeature`)
3. 💾 Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. 📤 Push a la rama (`git push origin feature/AmazingFeature`)
5. 🔃 Abre un Pull Request

---



<div align="center">

**Hecho con ❤️ para la comunidad Android**

⭐ Si te gusta este proyecto, ¡dale una estrella en GitHub!

</div>
