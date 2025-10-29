package com.blipblipcode.query.examples

import com.blipblipcode.query.QuerySelect
import com.blipblipcode.query.operator.Limit
import com.blipblipcode.query.operator.OrderBy
import com.blipblipcode.query.operator.SQLOperator

/**
 * Ejemplos de uso del operador LIMIT en QuerySelect
 */
class LimitExamples {

    /**
     * Ejemplo básico: Obtener los primeros 10 usuarios activos
     */
    fun getFirstTenActiveUsers(): QuerySelect {
        return QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .limit(10)
            .build()
        // SQL: SELECT * FROM users WHERE status = 'active' LIMIT 10
    }

    /**
     * Ejemplo con paginación: Obtener usuarios con offset
     */
    fun getUsersPaginated(page: Int, pageSize: Int = 20): QuerySelect {
        val offset = page * pageSize
        return QuerySelect.builder("users")
            .where(SQLOperator.Equals("status", "active"))
            .orderBy(OrderBy.Desc("created_at"))
            .limit(pageSize, offset)
            .build()
        // SQL: SELECT * FROM users WHERE status = 'active'
        //      ORDER BY created_at DESC
        //      LIMIT 20 OFFSET 40 (para page = 2, pageSize = 20)
    }

    /**
     * Ejemplo con campos específicos y límite
     */
    fun getTopUsersByScore(): QuerySelect {
        return QuerySelect.builder("users")
            .where(SQLOperator.GreaterThan("score", 100))
            .setFields("id", "name", "score")
            .orderBy(OrderBy.Desc("score"))
            .limit(5)
            .build()
        // SQL: SELECT id, name, score FROM users WHERE score > 100
        //      ORDER BY score DESC
        //      LIMIT 5
    }

    /**
     * Ejemplo usando objeto Limit directamente
     */
    fun getRecentPosts(): QuerySelect {
        val limitOperator = Limit(count = 15, offset = 0)
        return QuerySelect.builder("posts")
            .where(SQLOperator.Equals("published", true))
            .orderBy(OrderBy.Desc("published_date"))
            .limit(limitOperator)
            .build()
        // SQL: SELECT * FROM posts WHERE published = 1
        //      ORDER BY published_date DESC
        //      LIMIT 15
    }

    /**
     * Ejemplo de consulta compleja con múltiples condiciones y límite
     */
    fun getFilteredUsersWithLimit(): QuerySelect {
        return QuerySelect.builder("users")
            .where(SQLOperator.GreaterThan("age", 18))
            .and("status", SQLOperator.Equals("status", "active"))
            .and("country", SQLOperator.Equals("country", "Mexico"))
            .setFields("id", "name", "email", "age")
            .orderBy(OrderBy.Asc("name"))
            .limit(50, 10)
            .build()
        // SQL: SELECT id, name, email, age FROM users
        //      WHERE age > 18 AND status = 'active' AND country = 'Mexico'
        //      ORDER BY name ASC
        //      LIMIT 50 OFFSET 10
    }

    /**
     * Ejemplo de modificación de límite después de crear la consulta
     */
    fun dynamicLimit(): QuerySelect {
        val query = QuerySelect.builder("products")
            .where(SQLOperator.Equals("category", "electronics"))
            .orderBy(OrderBy.Desc("price"))
            .build()

        // Agregar límite dinámicamente
        query.limit(25, 5)

        return query
        // SQL: SELECT * FROM products WHERE category = 'electronics'
        //      ORDER BY price DESC
        //      LIMIT 25 OFFSET 5
    }
}
