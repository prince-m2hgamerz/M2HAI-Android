package com.m2h.m2haichatbot.di;

import android.content.Context;
import com.m2h.m2haichatbot.data.local.M2HAIDatabase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class AppModule_ProvideM2HAIDatabaseFactory implements Factory<M2HAIDatabase> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideM2HAIDatabaseFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public M2HAIDatabase get() {
    return provideM2HAIDatabase(contextProvider.get());
  }

  public static AppModule_ProvideM2HAIDatabaseFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideM2HAIDatabaseFactory(contextProvider);
  }

  public static M2HAIDatabase provideM2HAIDatabase(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideM2HAIDatabase(context));
  }
}
