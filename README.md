<div align="center">

# 🗄️ Android SQL Query Builder

### Una librería de Kotlin ligera y fluida para construir consultas SQL de forma programática

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-API%2021+-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Room](https://img.shields.io/badge/Room-Compatible-blue.svg?style=flat)](https://developer.android.com/training/data-storage/room)
[![JitPack](https://jitpack.io/v/blipblipcode/androidsqlquery.svg)](https://jitpack.io/#blipblipcode/androidsqlquery)


Diseñada para integrarse perfectamente con la base de datos de Android y Room a través de `@RawQuery`, permitiendo la creación de consultas dinámicas de manera segura y legible.

[Características](#-características) •
[Instalación](#-instalación) •
[Uso](#-uso) •
[Integración con Room](#-integración-con-room) •
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
- [📝 Ejemplos Avanzados](#-ejemplos-avanzados)
- [🤝 Contribuir](#-contribuir)
- [📄 Licencia](#-licencia)

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
    implementation 'com.github.blipblipcode:androidsqlquery:1.0.0'
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

val sqlString = selectQuery.asSql()
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
