package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.remote.NvidiaAIService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class AppModule_ProvideNvidiaAIServiceFactory implements Factory<NvidiaAIService> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<Auth> authProvider;

  public AppModule_ProvideNvidiaAIServiceFactory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider, Provider<Auth> authProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
    this.authProvider = authProvider;
  }

  @Override
  public NvidiaAIService get() {
    return provideNvidiaAIService(okHttpClientProvider.get(), jsonProvider.get(), authProvider.get());
  }

  public static AppModule_ProvideNvidiaAIServiceFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<Json> jsonProvider,
      Provider<Auth> authProvider) {
    return new AppModule_ProvideNvidiaAIServiceFactory(okHttpClientProvider, jsonProvider, authProvider);
  }

  public static NvidiaAIService provideNvidiaAIService(OkHttpClient okHttpClient, Json json,
      Auth auth) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNvidiaAIService(okHttpClient, json, auth));
  }
}
