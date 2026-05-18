package com.m2h.m2haichatbot.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.functions.Functions;
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
public final class SupabaseModule_ProvideSupabaseFunctionsFactory implements Factory<Functions> {
  private final Provider<SupabaseClient> clientProvider;

  public SupabaseModule_ProvideSupabaseFunctionsFactory(Provider<SupabaseClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public Functions get() {
    return provideSupabaseFunctions(clientProvider.get());
  }

  public static SupabaseModule_ProvideSupabaseFunctionsFactory create(
      Provider<SupabaseClient> clientProvider) {
    return new SupabaseModule_ProvideSupabaseFunctionsFactory(clientProvider);
  }

  public static Functions provideSupabaseFunctions(SupabaseClient client) {
    return Preconditions.checkNotNullFromProvides(SupabaseModule.INSTANCE.provideSupabaseFunctions(client));
  }
}
