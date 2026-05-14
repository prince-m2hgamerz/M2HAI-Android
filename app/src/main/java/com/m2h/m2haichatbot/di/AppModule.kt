package com.m2h.m2haichatbot.di

import android.content.Context
import androidx.room.Room
import com.m2h.m2haichatbot.data.local.M2HAIDatabase
import com.m2h.m2haichatbot.data.local.dao.AIModelDao
import com.m2h.m2haichatbot.data.local.dao.ChatDao
import com.m2h.m2haichatbot.data.local.dao.MessageDao
import com.m2h.m2haichatbot.data.remote.NvidiaAIService
import com.m2h.m2haichatbot.data.remote.SupabaseService
import com.m2h.m2haichatbot.data.repository.AuthRepositoryImpl
import com.m2h.m2haichatbot.data.repository.ChatRepositoryImpl
import com.m2h.m2haichatbot.data.repository.ModelRepositoryImpl
import com.m2h.m2haichatbot.domain.repository.AuthRepository
import com.m2h.m2haichatbot.domain.repository.ChatRepository as DomainChatRepository
import com.m2h.m2haichatbot.domain.repository.ModelRepository as DomainModelRepository
import com.m2h.m2haichatbot.utils.TelegramLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val telegramInterceptor = Interceptor { chain ->
            val request = chain.request()
            try {
                val response = chain.proceed(request)
                if (!response.isSuccessful && !request.url.toString().contains("telegram.org")) {
                    val errorMsg = "HTTP ${response.code} for ${request.method} ${request.url}"
                    TelegramLogger.logError(
                        message = errorMsg,
                        screen = "Network/API",
                        extra = "Request Body: ${request.body}"
                    )
                }
                response
            } catch (e: Exception) {
                if (!request.url.toString().contains("telegram.org")) {
                    TelegramLogger.logError(
                        message = "Network failure: ${e.message}",
                        throwable = e,
                        screen = "Network",
                        extra = "URL: ${request.url}"
                    )
                }
                throw e
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(telegramInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideM2HAIDatabase(@ApplicationContext context: Context): M2HAIDatabase {
        return Room.databaseBuilder(
            context,
            M2HAIDatabase::class.java,
            "m2hai_database"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideChatDao(database: M2HAIDatabase): ChatDao = database.chatDao()

    @Provides
    @Singleton
    fun provideMessageDao(database: M2HAIDatabase): MessageDao = database.messageDao()

    @Provides
    @Singleton
    fun provideAIModelDao(database: M2HAIDatabase): AIModelDao = database.aiModelDao()

    @Provides
    @Singleton
    fun provideSupabaseService(supabase: SupabaseClient): SupabaseService {
        return SupabaseService(supabase)
    }

    @Provides
    @Singleton
    fun provideNvidiaAIService(
        okHttpClient: OkHttpClient,
        json: Json,
        auth: Auth
    ): NvidiaAIService {
        return NvidiaAIService(okHttpClient, json, auth)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabaseService: SupabaseService,
        storage: Storage,
        auth: Auth
    ): AuthRepository {
        return AuthRepositoryImpl(supabaseService, storage, auth)
    }

    @Provides
    @Singleton
    fun provideChatRepository(
        supabaseService: SupabaseService,
        nvidiaAIService: NvidiaAIService,
        chatDao: ChatDao,
        messageDao: MessageDao,
        storage: Storage,
        auth: Auth,
        postgrest: Postgrest
    ): DomainChatRepository {
        return ChatRepositoryImpl(
            supabaseService,
            nvidiaAIService,
            chatDao,
            messageDao,
            storage,
            auth,
            postgrest
        )
    }

    @Provides
    @Singleton
    fun provideModelRepository(
        supabaseService: SupabaseService,
        aiModelDao: AIModelDao
    ): DomainModelRepository {
        return ModelRepositoryImpl(supabaseService, aiModelDao)
    }
}
