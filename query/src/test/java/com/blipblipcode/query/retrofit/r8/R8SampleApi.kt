package com.blipblipcode.query.retrofit.r8

import com.blipblipcode.query.retrofit.RepeatedQueryParameters
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap

/**
 * Minimal Retrofit service used as R8 program input by [R8ObfuscationTest].
 *
 * It lives in the test source set on purpose: it is fed to R8 together with
 * [RepeatedQueryParameters] so the type Retrofit reflects upon is the
 * R8 processed (renamed) one, exactly as in a real minified consumer app.
 */
interface R8SampleApi {

    @GET("search")
    fun search(@QueryMap options: RepeatedQueryParameters): Call<ResponseBody>
}
