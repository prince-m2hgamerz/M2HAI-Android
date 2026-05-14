package com.m2h.m2haichatbot.data.remote;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class SupabaseService_Factory implements Factory<SupabaseService> {
  private final Provider<SupabaseClient> supabaseProvider;

  public SupabaseService_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public SupabaseService get() {
    return newInstance(supabaseProvider.get());
  }

  public static SupabaseService_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new SupabaseService_Factory(supabaseProvider);
  }

  public static SupabaseService newInstance(SupabaseClient supabase) {
    return new SupabaseService(supabase);
  }
}
