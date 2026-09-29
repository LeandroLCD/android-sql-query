# ==============================================================================
# Reglas de consumo (se empaquetan en el AAR como proguard.txt y se aplican
# automaticamente en la app que use esta libreria cuando tenga R8/minify activo).
#
# Contexto: RepeatedQueryParameters se pasa a Retrofit como @QueryMap. Retrofit
# resuelve el tipo generico de la clase por reflexion (getGenericSuperclass) para
# exigir que las claves sean String. Si R8 elimina el atributo Signature, la
# clase queda como una Class cruda y las variables de tipo K/V de LinkedHashMap
# se pierden, produciendo en tiempo de ejecucion:
#
#   @QueryMap keys must be of type String: K (parameter #2)
#   for method IMyApi.search
# ==============================================================================

# Atributos requeridos por la reflexion de Retrofit.
# - Signature: sin esto, getGenericSuperclass() devuelve Class en vez de
#   ParameterizedType y se pierde el binding String/Any.
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*
-keepattributes InnerClasses,EnclosingMethod

# RepeatedQueryParameters es inspeccionada por reflexion por Retrofit.
# Se permite renombrar la clase (allowobfuscation) y eliminarla si el consumidor
# no la usa (allowshrinking); lo que no se negocia es la firma generica del
# padre (LinkedHashMap<String, Any>) ni la superficie de metodos publicos.
-keep,allowobfuscation,allowshrinking class com.blipblipcode.query.retrofit.RepeatedQueryParameters
-keepclassmembers,allowobfuscation class com.blipblipcode.query.retrofit.RepeatedQueryParameters {
    public <init>(...);
    *;
}
-keep,allowobfuscation,allowshrinking class com.blipblipcode.query.retrofit.RepeatedQueryParameters$SimpleEntry
