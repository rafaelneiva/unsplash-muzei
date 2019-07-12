/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rafaelneiva.muzeiunsplash.muzeiunsplash

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import com.rafaelneiva.muzeiunsplash.ATTRIBUTION_QUERY_PARAMETERS
import com.rafaelneiva.muzeiunsplash.CONSUMER_KEY
import com.rafaelneiva.muzeiunsplash.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.*

interface UnsplashService {

    companion object {

        fun createService(): UnsplashService {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    var request = chain.request()
                    val url = request.url().newBuilder().addQueryParameter("client_id", CONSUMER_KEY).build()
                    request = request.newBuilder().url(url).build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.unsplash.com/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create<UnsplashService>(UnsplashService::class.java)
        }

        @Throws(IOException::class)
        internal fun popularPhotos(): List<Photo> {
            return createService().popularPhotos.execute().body()
                ?: throw IOException("Response was null")
        }

        private const val defaultCategory: Int = 540518

        @Throws(IOException::class)
        internal fun randomPhotosByCategories(context: Context): List<Photo> {
            val collectionId = PreferenceManager.getDefaultSharedPreferences(context).getInt(context.getString(R.string.shpr_collection_id), defaultCategory)
            return createService().collectionPhotos(collectionId.toString()).execute().body()
                ?: throw IOException("Response was null")
        }

        @Throws(IOException::class)
        internal fun trackDownload(photoId: String) {
            createService().trackDownload(photoId).execute()
        }

        internal fun getCollections(): LiveData<PagedList<Collection>> {
            val config = PagedList.Config.Builder()
                .setPageSize(30)
                .setInitialLoadSizeHint(30)
                .setEnablePlaceholders(false)
                .build()
            return LivePagedListBuilder<Int, Collection>(CollectionDataSourceFactory(), config).build()
        }
    }

    @get:GET("photos/curated?order_by=popular&per_page=30")
    val popularPhotos: Call<List<Photo>>

    @GET("collections/{collectionId}/photos?per_page=100")
    fun collectionPhotos(@Path("collectionId") collection: String): Call<List<Photo>>

    @GET("photos/{id}/download")
    fun trackDownload(@Path("id") photoId: String): Call<Any>

    @GET("collections/featured?per_page=10")
    fun getCollections(@Query("page") page: Int): Call<List<Collection>>

    data class Photo(
        val id: String,
        val urls: Urls,
        val description: String?,
        val user: User,
        val links: Links
    )

    data class Urls(
        val full: String,
        val small: String
    )

    data class Links(val html: String) {
        val webUri get() = "$html$ATTRIBUTION_QUERY_PARAMETERS".toUri()
    }

    data class User(
        val name: String,
        val links: Links
    )

    data class Collection(
        val id: Int,
        val title: String,
        val description: String,
        val total_photos: Int,
        val cover_photo: Photo,
        val updated_at: Date
    )
}

class CollectionDataSource : PageKeyedDataSource<Int, UnsplashService.Collection>() {

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Int, UnsplashService.Collection>) {
        val initialPage = 1
        UnsplashService.createService().getCollections(initialPage).enqueue(object : Callback<List<UnsplashService.Collection>> {
            override fun onFailure(call: Call<List<UnsplashService.Collection>>, t: Throwable) {
                throw IOException(t.localizedMessage)
            }

            override fun onResponse(call: Call<List<UnsplashService.Collection>>, response: Response<List<UnsplashService.Collection>>) {
                if (response.isSuccessful) {
                    callback.onResult(response.body()!!, initialPage, response.headers().get("X-Total")?.toInt()!!,null, initialPage + 1)
                }
            }

        })
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashService.Collection>) {
        val page = params.key
        UnsplashService.createService().getCollections(page).enqueue(object : Callback<List<UnsplashService.Collection>> {
            override fun onFailure(call: Call<List<UnsplashService.Collection>>, t: Throwable) {
                throw IOException(t.localizedMessage)
            }

            override fun onResponse(call: Call<List<UnsplashService.Collection>>, response: Response<List<UnsplashService.Collection>>) {
                if (response.isSuccessful) {
                    callback.onResult(response.body()!!, page + 1)
                }
            }

        })
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, UnsplashService.Collection>) {
    }
}

class CollectionDataSourceFactory() : DataSource.Factory<Int, UnsplashService.Collection>() {
    private val usersDataSourceLiveData = MutableLiveData<CollectionDataSource>()

    override fun create(): DataSource<Int, UnsplashService.Collection> {
        val usersDataSource = CollectionDataSource()
        usersDataSourceLiveData.postValue(usersDataSource)
        return usersDataSource
    }
}
