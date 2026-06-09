# Testing Guidelines for Agents

## 1. Test Naming Convention

Test names must follow **snake_case** format and clearly describe:
- **What** is expected to happen (should)
- **What behavior** is being tested
- **When** that behavior occurs
    - **Which method** is being tested (in_invoke, in_execute, etc.)

### Format:
```
should_{action}_whent_{condition}_in_{method}
```

### Examples:
```kotlin
@Test
fun should_remove_item_from_cart_when_quantity_is_zero_in_invoke()

@Test
fun should_throw_QuantityMustBePositive_when_quantity_is_negative_in_invoke()

@Test
fun should_update_quantity_when_quantity_is_valid_and_stock_is_sufficient_in_invoke()
```

## 2. Test Structure: Given-When-Then

All tests must be divided into **3 clearly identified sections** with comments:

```kotlin
@Test
fun should_do_something_when_condition_in_invoke() = runTest {
    //GIVEN
    // Preparation: variables, mocks, initial setup
    
    //WHEN
    // Execution: call to the method being tested
    
    //THEN
    // Verification: asserts and behavior verifications
}
```

### Complete example:
```kotlin
@Test
fun should_update_quantity_when_quantity_is_valid_and_stock_is_sufficient_in_invoke() = runTest {
    //GIVEN
    val productId = "1"
    val quantity = 3
    val product = productBuilder {
        withId(productId)
        withStock(5)
    }
    every { productRepository.getProductById(productId) } returns flowOf(product)
    
    //WHEN
    useCase.invoke(productId, quantity)
    
    //THEN
    coVerify { cartItemRepository.updateQuantity(productId, quantity) }
}
```

## 3. Exception Testing

To test exceptions, **use `runCatching`** to capture the result and verify the exception type:

```kotlin
@Test
fun should_throw_QuantityMustBePositive_when_quantity_is_negative_in_invoke() = runTest {
    //GIVEN
    val productId = "1"
    val quantity = -1
    
    //WHEN
    val result = runCatching { useCase.invoke(productId, quantity) }
    
    //THEN
    assert(result.exceptionOrNull() is AppError.Validation.QuantityMustBePositive)
}
```

### Advantages of runCatching vs assertThrows:
- More idiomatic in Kotlin
- Allows easy inspection of exception properties
- Cleaner and clearer syntax

### Verifying exception properties:
```kotlin
@Test
fun should_throw_InsufficientStock_when_quantity_exceeds_product_stock_in_invoke() = runTest {
    //GIVEN
    val productId = "1"
    val quantity = 10
    val product = productBuilder {
        withId(productId)
        withStock(5)
    }
    every { productRepository.getProductById(productId) } returns flowOf(product)
    
    //WHEN
    val result = runCatching { useCase.invoke(productId, quantity) }
    
    //THEN
    val exception = result.exceptionOrNull() as? AppError.Validation.InsufficientStock
    assert(exception != null)
    assertEquals(5, exception?.available)
}
```

## 4. Builder Pattern for Object Creation

To create test objects, **use the Builder pattern** with default values that allow easy creation of valid objects.

### Location:
Builders must be in a package named `builder` at the root of the corresponding test directory.

Example path:
```
app/src/test/java/com/package/feature/domain/usecase/builder/
```

### Builder Structure:

```kotlin
class CartItemBuilder {
    private var productId: String = "product-1"
    private var quantity: Int = 2

    fun withProductId(productId: String) = apply { this.productId = productId }
    fun withQuantity(quantity: Int) = apply { this.quantity = quantity }

    fun build(): CartItem {
        return CartItem(productId, quantity)
    }
}

fun cartItem(block: CartItemBuilder.() -> Unit): CartItem = 
    CartItemBuilder().apply(block).build()
```

### Using the Builder in Tests:

#### Case 1: Using default values
```kotlin
//GIVEN
val cartItem = cartItem { }
```

#### Case 2: Customizing some values
```kotlin
//GIVEN
val cartItem = cartItem {
    withProductId("custom-id")
    withQuantity(5)
}
```

#### Case 3: Customizing only one value
```kotlin
//GIVEN
val cartItem = cartItem {
    withQuantity(10)
}
// productId will have the default value "product-1"
```

### Builder Pattern Advantages:
- **Default values**: Tests are more concise, you only specify what matters
- **Readability**: Code is self-documenting
- **Maintainability**: If the model changes, you only update the builder
- **Flexibility**: Easy to create different test scenarios

## 5. Complete Test Example

```kotlin
class UpdateCartItemUseCaseTest {

    private lateinit var cartItemRepository: CartItemRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var useCase: UpdateCartItemUseCase

    @Before
    fun setUp() {
        cartItemRepository = mockk(relaxed = true)
        productRepository = mockk()
        useCase = UpdateCartItemUseCase(cartItemRepository, productRepository)
    }

    @Test
    fun should_throw_QuantityMustBePositive_when_quantity_is_negative_in_invoke() = runTest {
        //GIVEN
        val productId = "1"
        val quantity = -1
        
        //WHEN
        val result = runCatching { useCase.invoke(productId, quantity) }
        
        //THEN
        assert(result.exceptionOrNull() is AppError.Validation.QuantityMustBePositive)
    }

    @Test
    fun should_remove_item_from_cart_when_quantity_is_zero_in_invoke() = runTest {
        //GIVEN
        val productId = "1"
        val quantity = 0
        
        //WHEN
        useCase.invoke(productId, quantity)
        
        //THEN
        coVerify { cartItemRepository.removeFromCart(productId) }
        coVerify(exactly = 0) { cartItemRepository.updateQuantity(any(), any()) }
    }

    @Test
    fun should_update_quantity_when_quantity_is_valid_and_stock_is_sufficient_in_invoke() = runTest {
        //GIVEN
        val productId = "1"
        val quantity = 3
        val product = productBuilder {
            withId(productId)
            withStock(5)
        }
        every { productRepository.getProductById(productId) } returns flowOf(product)
        
        //WHEN
        useCase.invoke(productId, quantity)
        
        //THEN
        coVerify { cartItemRepository.updateQuantity(productId, quantity) }
    }
}
```

## 6. Best Practices Summary

✅ **DO:**
- Use descriptive names in snake_case with `_in_{method}` suffix
- Divide tests into GIVEN-WHEN-THEN
- Use `runCatching` to capture exceptions
- Use builders to create test objects
- Define sensible default values in builders
- Use `runTest` for tests with coroutines

❌ **DON'T:**
- Never include comments in the code
- Use backticks in test names
- Use `assertThrows` (prefer `runCatching`)
- Create objects manually in each test
- Use `mockk(relaxed = true)` for repositories 
- Mix preparation, execution, and verification logic
- Omit the method name in the test name


