package com.m2h.m2haichatbot.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import kotlinx.serialization.json.Json;
import okhttp3.OkHttpClient;

@ScopeMetadata
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
public final class NvidiaAIService_Factory implements Factory<NvidiaAIService> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<Json> jsonProvider;

  private final Provider<Auth> authProvider;

  public NvidiaAIService_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider, Provider<Auth> authProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
    this.authProvider = authProvider;
  }

  @Override
  public NvidiaAIService get() {
    return newInstance(okHttpClientProvider.get(), jsonProvider.get(), authProvider.get());
  }

  public static NvidiaAIService_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider, Provider<Auth> authProvider) {
    return new NvidiaAIService_Factory(okHttpClientProvider, jsonProvider, authProvider);
  }

  public static NvidiaAIService newInstance(OkHttpClient okHttpClient, Json json, Auth auth) {
    return new NvidiaAIService(okHttpClient, json, auth);
  }
}
