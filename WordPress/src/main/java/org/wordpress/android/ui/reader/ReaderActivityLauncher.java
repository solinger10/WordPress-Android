package org.wordpress.android.ui.reader;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.ReaderComment;
import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.reader.ReaderTypes.ReaderPostListType;
import org.wordpress.android.util.ToastUtils;

public class ReaderActivityLauncher {

    /*
     * show a single reader post in the detail view - simply calls showReaderPostPager
     * with a single post
     */
    public static void showReaderPostDetail(Context context, long blogId, long postId) {
        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);
        intent.putExtra(ReaderConstants.ARG_IS_SINGLE_POST, true);

        if (context instanceof Activity) {
            // For ActionBarActivity subclasses, we need to pull the title from the Toolbar
            CharSequence title = null;
            if (context instanceof ActionBarActivity && ((ActionBarActivity) context).getSupportActionBar() != null) {
                title = ((ActionBarActivity) context).getSupportActionBar().getTitle();
            }

            if (title == null) {
                // Not an ActionBarActivity, or getSupportActionBar().getTitle() returned null.
                // Try to read the title from the Activity
                title = ((Activity)context).getTitle();
            }
            intent.putExtra(ReaderConstants.ARG_TITLE, title);
        }

        showReaderPostPager(context, intent);
    }

    /*
     * show pager view of posts with a specific tag - passed blogId/postId is the post
     * to select after the pager is populated
     */
    public static void showReaderPostPagerForTag(Context context,
                                                 ReaderTag tag,
                                                 ReaderPostListType postListType,
                                                 long blogId,
                                                 long postId) {
        if (tag == null) {
            return;
        }

        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, postListType);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_TITLE, tag.getCapitalizedTagName());
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);

        showReaderPostPager(context, intent);
    }

    /*
     * show pager view of posts in a specific blog
     */
    public static void showReaderPostPagerForBlog(Context context,
                                                  long blogId,
                                                  long postId) {
        Intent intent = new Intent(context, ReaderPostPagerActivity.class);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        intent.putExtra(ReaderConstants.ARG_TITLE, context.getString(R.string.reader_title_blog_preview));
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, postId);

        showReaderPostPager(context, intent);
    }

    private static void showReaderPostPager(Context context, Intent intent) {
        if (context instanceof Activity) {
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    R.anim.reader_activity_slide_in,
                    R.anim.do_nothing);
            ActivityCompat.startActivity((Activity) context, intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    /*
     * show a list of posts in a specific blog
     */
    public static void showReaderBlogPreview(Context context, long blogId) {
        if (blogId == 0) {
            return;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_PREVIEW);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, blogId);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    public static void showReaderBlogPreview(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        if (post.isExternal) {
            showReaderFeedPreview(context, post.feedId);
        } else {
            showReaderBlogPreview(context, post.blogId);
        }
    }

    public static void showReaderFeedPreview(Context context, long feedId) {
        if (feedId == 0) {
            return;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_BLOG_PREVIEW);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_FEED_ID, feedId);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.BLOG_PREVIEW);
        context.startActivity(intent);
    }

    /*
     * show a list of posts with a specific tag
     */
    public static void showReaderTagPreview(Context context, ReaderTag tag) {
        if (tag == null) {
            return;
        }
        AnalyticsTracker.track(AnalyticsTracker.Stat.READER_TAG_PREVIEW);
        Intent intent = new Intent(context, ReaderPostListActivity.class);
        intent.putExtra(ReaderConstants.ARG_TAG, tag);
        intent.putExtra(ReaderConstants.ARG_POST_LIST_TYPE, ReaderPostListType.TAG_PREVIEW);
        context.startActivity(intent);
    }

    /*
     * show comments for the passed post
     */
    public static void showReaderComments(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderCommentListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);

        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            ActivityOptionsCompat options = ActivityOptionsCompat.makeCustomAnimation(
                    activity,
                    R.anim.reader_flyin,
                    R.anim.reader_activity_scale_out);
            ActivityCompat.startActivity(activity, intent, options.toBundle());
        } else {
            context.startActivity(intent);
        }
    }

    /*
     * show users who liked the passed post
     */
    public static void showReaderLikingUsers(Context context, ReaderPost post) {
        if (post == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);
        context.startActivity(intent);
    }

    /*
     * show users who liked the passed comment
     */
    public static void showReaderLikingUsers(Context context, ReaderComment comment) {
        if (comment == null) {
            return;
        }
        Intent intent = new Intent(context, ReaderUserListActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, comment.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, comment.postId);
        intent.putExtra(ReaderConstants.ARG_COMMENT_ID, comment.commentId);
        context.startActivity(intent);
    }

    /*
     * show followed tags & blogs
     */
    public static void showReaderSubsForResult(Activity activity) {
        Intent intent = new Intent(activity, ReaderSubsActivity.class);
        activity.startActivityForResult(intent, RequestCodes.READER_SUBS);
    }

    /*
     * show the passed imageUrl in the fullscreen photo activity - optional content is the
     * content of the post the image is in, used by the activity to show all images in
     * the post
     */
    public static void showReaderPhotoViewer(Context context,
                                             String imageUrl,
                                             String content,
                                             View sourceView,
                                             boolean isPrivate,
                                             int startX,
                                             int startY) {
        if (context == null || TextUtils.isEmpty(imageUrl)) {
            return;
        }

        Intent intent = new Intent(context, ReaderPhotoViewerActivity.class);
        intent.putExtra(ReaderConstants.ARG_IMAGE_URL, imageUrl);
        intent.putExtra(ReaderConstants.ARG_IS_PRIVATE, isPrivate);
        if (!TextUtils.isEmpty(content)) {
            intent.putExtra(ReaderConstants.ARG_CONTENT, content);
        }

        if (context instanceof Activity) {
            // use built-in scale animation on jb+, fall back to default animation on pre-jb
            Activity activity = (Activity) context;
            if (sourceView != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityOptionsCompat options =
                        ActivityOptionsCompat.makeScaleUpAnimation(sourceView, startX, startY, 0, 0);
                ActivityCompat.startActivity(activity, intent, options.toBundle());
            } else {
                activity.startActivity(intent);
            }
        } else {
            context.startActivity(intent);
        }
    }

    /*
     * show the reblog activity for the passed post
     */
    public static void showReaderReblogForResult(Activity activity, ReaderPost post, View source) {
        if (activity == null || post == null) {
            return;
        }
        Intent intent = new Intent(activity, ReaderReblogActivity.class);
        intent.putExtra(ReaderConstants.ARG_BLOG_ID, post.blogId);
        intent.putExtra(ReaderConstants.ARG_POST_ID, post.postId);
        ActivityOptionsCompat options;
        if (source != null) {
            int startX = source.getLeft();
            int startY = source.getTop();
            options = ActivityOptionsCompat.makeScaleUpAnimation(source, startX, startY, 0, 0);
        } else {
            options = ActivityOptionsCompat.makeCustomAnimation(activity, R.anim.reader_flyin, 0);
        }
        ActivityCompat.startActivityForResult(activity, intent, RequestCodes.READER_REBLOG, options.toBundle());

    }

    public static enum OpenUrlType { INTERNAL, EXTERNAL }
    public static void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }
    public static void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (openUrlType == OpenUrlType.INTERNAL) {
            // Open the URL by using the internal browser without authenticating to wpcom.
            // See: https://github.com/wordpress-mobile/WordPress-Android/issues/1921
            // If you pass a wpcom URL that needs authentication to be viewed, it will work since
            // the reader authenticates to wpcom at startup by calling ReaderAuthActions.updateCookies
            WPWebViewActivity.openURL(context, url);
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                ToastUtils.showToast(context, context.getString(R.string.reader_toast_err_url_intent, url), ToastUtils.Duration.LONG);
            }
        }
    }
}
