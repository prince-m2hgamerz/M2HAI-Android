package com.m2h.m2haichatbot.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.storage.Storage;
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
public final class SupabaseModule_ProvideSupabaseStorageFactory implements Factory<Storage> {
  private final Provider<SupabaseClient> clientProvider;

  public SupabaseModule_ProvideSupabaseStorageFactory(Provider<SupabaseClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public Storage get() {
    return provideSupabaseStorage(clientProvider.get());
  }

  public static SupabaseModule_ProvideSupabaseStorageFactory create(
      Provider<SupabaseClient> clientProvider) {
    return new SupabaseModule_ProvideSupabaseStorageFactory(clientProvider);
  }

  public static Storage provideSupabaseStorage(SupabaseClient client) {
    return Preconditions.checkNotNullFromProvides(SupabaseModule.INSTANCE.provideSupabaseStorage(client));
  }
}
