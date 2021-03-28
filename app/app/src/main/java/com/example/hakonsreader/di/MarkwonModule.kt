package com.example.hakonsreader.di

import android.content.Context
import com.example.hakonsreader.api.utils.MarkdownAdjuster
import com.example.hakonsreader.markwonplugins.*
import com.squareup.picasso.Picasso
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.spans.LinkSpan
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.ImageProps
import io.noties.markwon.image.picasso.PicassoImagesPlugin
import org.commonmark.node.Image
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Annotation to inject a [Markwon] instance that has an image plugin
 *
 * @see MarkwonWithoutImages
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MarkwonWithImages

/**
 * Annotation to inject a [Markwon] instance that does not have an image plugin
 *
 * @see MarkwonWithImages
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MarkwonWithoutImages


/**
 * Annotation to inject a [MarkdownAdjuster] instance that adjusts images to be markdown images
 *
 * @see AdjusterWithoutImages
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdjusterWithImages

/**
 * Annotation to inject a [MarkdownAdjuster] instance that does not adjust images to be markdown images
 *
 * @see AdjusterWithImages
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AdjusterWithoutImages


/**
 * Module for providing [Markwon] and [MarkdownAdjuster] instances.
 *
 * Two instances are provided, one with image support and one without. Use [MarkwonWithImages]/[AdjusterWithImages] and
 * [MarkwonWithoutImages]/[AdjusterWithoutImages] to inject different instances
 */
@InstallIn(SingletonComponent::class)
@Module
object MarkwonModule {

    /**
     * Returns a base Markwon builder that does not include any image plugins
     */
    private fun baseMarkwon(context: Context) : Markwon.Builder {
        return Markwon.builder(context)
                // Headers, blockquote etc. are a part of the core
                .usePlugin(CorePlugin.create())
                .usePlugin(TablePlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())

                // Custom plugins
                .usePlugin(RedditSpoilerPlugin())
                .usePlugin(RedditLinkPlugin())
                .usePlugin(SuperscriptPlugin())
                .usePlugin(LinkPlugin())
                .usePlugin(EnlargeLinkPlugin())
                .usePlugin(ThemePlugin(context))
    }

    @MarkwonWithImages
    @Singleton
    @Provides
    fun provideMarkwonWithImages(@ApplicationContext context: Context) : Markwon {
        return baseMarkwon(context)
                .usePlugin(PicassoImagesPlugin.create(Picasso.get()))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                        builder.appendFactory(Image::class.java) { configuration, props ->
                            val url = ImageProps.DESTINATION.require(props)

                            LinkSpan(
                                    configuration.theme(),
                                    url,
                                    // Not sure how this is supposed to work as even if I define my own custom
                                    // resolver it still uses the default
                                    // Might have something to do with how I set the LinkMovementMethod?
                                    // I want to open the image with a SharedElementTransition like other images
                                    // Might be impossible since it seems as the images are actually a TextView, not
                                    // ImageView (from layout inspector)
                                    configuration.linkResolver()
                            )
                        }
                    }
                })
                .build()
    }

    @MarkwonWithoutImages
    @Singleton
    @Provides
    fun provideMarkwonNoImages(@ApplicationContext context: Context) : Markwon {
        return baseMarkwon(context).build()
    }


    @AdjusterWithImages
    @Singleton
    @Provides
    fun createMarkdownAdjusterWithImages() : MarkdownAdjuster {
        return MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .checkUrlEncoding()
                .convertImageLinksToMarkdown()
                .build()
    }

    @AdjusterWithoutImages
    @Singleton
    @Provides
    fun createMarkdownAdjusterNoImages() : MarkdownAdjuster {
        return MarkdownAdjuster.Builder()
                .checkHeaderSpaces()
                .checkUrlEncoding()
                .build()
    }
}