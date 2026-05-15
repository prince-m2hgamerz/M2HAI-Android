package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.remote.NvidiaAIService;
import com.m2h.m2haichatbot.data.repository.PreferenceRepository;
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

  private final Provider<PreferenceRepository> preferenceRepositoryProvider;

  public AppModule_ProvideNvidiaAIServiceFactory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<Json> jsonProvider, Provider<Auth> authProvider,
      Provider<PreferenceRepository> preferenceRepositoryProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.jsonProvider = jsonProvider;
    this.authProvider = authProvider;
    this.preferenceRepositoryProvider = preferenceRepositoryProvider;
  }

  @Override
  public NvidiaAIService get() {
    return provideNvidiaAIService(okHttpClientProvider.get(), jsonProvider.get(), authProvider.get(), preferenceRepositoryProvider.get());
  }

  public static AppModule_ProvideNvidiaAIServiceFactory create(
      Provider<OkHttpClient> okHttpClientProvider, Provider<Json> jsonProvider,
      Provider<Auth> authProvider, Provider<PreferenceRepository> preferenceRepositoryProvider) {
    return new AppModule_ProvideNvidiaAIServiceFactory(okHttpClientProvider, jsonProvider, authProvider, preferenceRepositoryProvider);
  }

  public static NvidiaAIService provideNvidiaAIService(OkHttpClient okHttpClient, Json json,
      Auth auth, PreferenceRepository preferenceRepository) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideNvidiaAIService(okHttpClient, json, auth, preferenceRepository));
  }
}
