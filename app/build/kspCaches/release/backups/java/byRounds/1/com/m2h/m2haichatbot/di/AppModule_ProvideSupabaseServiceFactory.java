package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.remote.SupabaseService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class AppModule_ProvideSupabaseServiceFactory implements Factory<SupabaseService> {
  private final Provider<SupabaseClient> supabaseProvider;

  public AppModule_ProvideSupabaseServiceFactory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public SupabaseService get() {
    return provideSupabaseService(supabaseProvider.get());
  }

  public static AppModule_ProvideSupabaseServiceFactory create(
      Provider<SupabaseClient> supabaseProvider) {
    return new AppModule_ProvideSupabaseServiceFactory(supabaseProvider);
  }

  public static SupabaseService provideSupabaseService(SupabaseClient supabase) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSupabaseService(supabase));
  }
}
