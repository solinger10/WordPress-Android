package org.wordpress.android.ui.reader.parsers;

import android.text.TextUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderPostList;
import org.wordpress.android.ui.reader.utils.ReaderImageScanner;
import org.wordpress.android.util.DateTimeUtils;

import java.text.BreakIterator;
import java.util.Map;
import java.util.Set;

/**
 * parser for endpoints which return a list of posts
 */
public class ReaderPostListParser {

    private String mJsonString;
    private JsonElement mRootElement;

    public ReaderPostListParser(final String jsonInput) {
        mJsonString = jsonInput;
    }

    private JsonElement getRootElement() {
        if (mRootElement == null) {
            mRootElement = new JsonParser().parse(mJsonString);
        }
        return mRootElement;
    }

    public ReaderPostList parse() {
        ReaderPostList posts = new ReaderPostList();
        JsonArray jsonPostArray = getRootElement().getAsJsonObject().getAsJsonArray("posts");
        for (JsonElement jsonPost: jsonPostArray) {
            ReaderPost post = parseSinglePost(jsonPost);
            posts.add(post);
        }

        return posts;
    }

    private ReaderPost parseSinglePost(JsonElement jsonPost) {
        ReaderPost post = new ReaderPost();
        JsonObject json = jsonPost.getAsJsonObject();

        post.postId = GsonUtils.getLong(json, "ID");
        post.blogId = GsonUtils.getLong(json, "site_ID");

        if (json.has("pseudo_ID")) {
            post.setPseudoId(GsonUtils.getString(json, "pseudo_ID"));  // read/ endpoint
        } else {
            post.setPseudoId(GsonUtils.getString(json, "global_ID"));  // sites/ endpoint
        }

        post.setText(GsonUtils.getString(json, "text"));
        post.setExcerpt(GsonUtils.getStringStripHtml(json, "excerpt"));
        post.setTitle(GsonUtils.getStringStripHtml(json, "title"));
        post.setUrl(GsonUtils.getString(json, "URL"));
        post.setBlogUrl(GsonUtils.getString(json, "site_URL"));

        post.numReplies = GsonUtils.getInt(json, "comment_count");
        post.numLikes = GsonUtils.getInt(json, "like_count");

        post.isLikedByCurrentUser = GsonUtils.getBool(json, "i_like");
        post.isFollowedByCurrentUser = GsonUtils.getBool(json, "is_following");
        post.isRebloggedByCurrentUser = GsonUtils.getBool(json, "is_reblogged");
        post.isCommentsOpen = GsonUtils.getBool(json, "comments_open");
        post.isExternal = GsonUtils.getBool(json, "is_external");
        post.isPrivate = GsonUtils.getBool(json, "site_is_private");

        post.isLikesEnabled = GsonUtils.getBool(json, "likes_enabled");
        post.isSharingEnabled = GsonUtils.getBool(json, "sharing_enabled");

        JsonElement elemAuthor = json.get("author");
        if (elemAuthor != null) {
            JsonObject objAuthor = elemAuthor.getAsJsonObject();
            post.authorId = GsonUtils.getLong(objAuthor, "ID");
            post.setAuthorName(GsonUtils.getString(objAuthor, "name"));
            post.setPostAvatar(GsonUtils.getString(objAuthor, "avatar_URL"));
            // site_URL doesn't exist for /sites/ endpoints, so get it from the author
            if (!post.hasBlogUrl()) {
                post.setBlogUrl(GsonUtils.getString(objAuthor, "URL"));
            }
        }

        // only freshly-pressed posts have the "editorial" section
        JsonElement elemEditorial = json.get("editorial");
        if (elemEditorial != null) {
            JsonObject objEditorial = elemEditorial.getAsJsonObject();
            post.blogId = GsonUtils.getLong(objEditorial, "blog_id");
            post.setBlogName(GsonUtils.getStringDecoded(objEditorial, "blog_name"));
            post.setFeaturedImage(ReaderImageScanner.getImageUrlFromFPFeaturedImageUrl(GsonUtils.getString(objEditorial, "image")));
            post.setPrimaryTag(GsonUtils.getString(objEditorial, "highlight_topic_title"));
            // we want freshly-pressed posts to show & store the date they were chosen rather than the day they were published
            post.setPublished(GsonUtils.getString(objEditorial, "displayed_on"));
        } else {
            post.setFeaturedImage(GsonUtils.getString(json, "featured_image"));
            post.setBlogName(GsonUtils.getStringDecoded(json, "site_name"));
            post.setPublished(GsonUtils.getString(json, "date"));
        }

        // the date a post was liked is only returned by the read/liked/ endpoint - if this exists,
        // set it as the timestamp so posts are sorted by the date they were liked rather than the
        // date they were published (the timestamp is used to sort posts when querying)
        String likeDate = GsonUtils.getString(json, "date_liked");
        if (!TextUtils.isEmpty(likeDate)) {
            post.timestamp = DateTimeUtils.iso8601ToTimestamp(likeDate);
        } else {
            post.timestamp = DateTimeUtils.iso8601ToTimestamp(post.getPublished());
        }

        // if there's no featured thumbnail, check if featured media has been set - this is sometimes
        // a YouTube or Vimeo video, in which case store it as the featured video so we can treat
        // it as a video
        if (!post.hasFeaturedImage() && json.has("featured_media")) {
            JsonObject objMedia = json.getAsJsonObject("featured_media");
            String mediaUrl = GsonUtils.getString(objMedia, "uri");
            if (!TextUtils.isEmpty(mediaUrl)) {
                String type = GsonUtils.getString(objMedia, "type");
                boolean isVideo = (type != null && type.equals("video"));
                if (isVideo) {
                    post.setFeaturedVideo(mediaUrl);
                } else {
                    post.setFeaturedImage(mediaUrl);
                }
            }

            // if we still don't have a featured image, parse the content for an image that's
            // suitable as a featured image
            if (!post.hasFeaturedImage()) {
                ReaderImageScanner scanner = new ReaderImageScanner(post.getText(), post.isPrivate);
                post.setFeaturedImage(scanner.getBestFeaturedImage());
            }
        }

        // if the post is untitled, make up a title from the excerpt
        if (!post.hasTitle() && post.hasExcerpt()) {
            post.setTitle(extractTitle(post.getExcerpt(), 50));
        }

        if (json.has("tags")) {
            assignTags(post, json.getAsJsonObject("tags"));
        }

        if (json.has("attachments")) {
            JsonObject objAttachments = json.getAsJsonObject("attachments");
            post.setAttachmentsJson(objAttachments.toString());
        }

        // site metadata - returned when ?meta=site was added to the request
        JsonObject objSite = GsonUtils.getChild(json, "meta/data/site");
        if (objSite != null) {
            post.blogId = GsonUtils.getLong(objSite, "ID");
            post.setBlogName(GsonUtils.getString(objSite, "name"));
            post.setBlogUrl(GsonUtils.getString(objSite, "URL"));
            post.isPrivate = GsonUtils.getBool(objSite, "is_private");
            // TODO: as of 29-Sept-2014, this is broken - endpoint returns false when it should be true
            post.isJetpack = GsonUtils.getBool(objSite, "jetpack");
        }

        return post;
    }

    /*
     * extracts a title from a post's excerpt - used when the post has no title
     */
    private static String extractTitle(final String excerpt, int maxLen) {
        if (TextUtils.isEmpty(excerpt)) {
            return null;
        }
        if (excerpt.length() < maxLen) {
            return excerpt.trim();
        }

        StringBuilder result = new StringBuilder();
        BreakIterator wordIterator = BreakIterator.getWordInstance();
        wordIterator.setText(excerpt);
        int start = wordIterator.first();
        int end = wordIterator.next();
        int totalLen = 0;
        while (end != BreakIterator.DONE) {
            String word = excerpt.substring(start, end);
            result.append(word);
            totalLen += word.length();
            if (totalLen >= maxLen) {
                break;
            }
            start = end;
            end = wordIterator.next();
        }

        if (totalLen == 0) {
            return null;
        }

        return result.toString().trim() + "...";
    }

    /*
     * assigns primary/secondary tags to the passed post from the passed JSON "tags" object
     */
    private static void assignTags(ReaderPost post, JsonObject jsonTags) {
        if (jsonTags == null) {
            return;
        }

        Set<Map.Entry<String, JsonElement>> entries = jsonTags.entrySet();
        if (entries.isEmpty()) {
            return;
        }

        // most popular tag & second most popular tag, based on usage count on this blog
        String mostPopularTag = null;
        String nextMostPopularTag = null;
        int popularCount = 0;

        for (Map.Entry<String, JsonElement> entry : entries) {
            JsonObject objTag = entry.getValue().getAsJsonObject();

            // if the number of posts on this blog that use this tag is higher than previous,
            // set this as the most popular tag, and set the second most popular tag to
            // the current most popular tag
            int postCount = GsonUtils.getInt(objTag, "post_count");
            if (postCount > popularCount) {
                nextMostPopularTag = mostPopularTag;
                mostPopularTag = GsonUtils.getString(objTag, "name");
                popularCount = postCount;
            }
        }


        // don't set primary tag if one is already set (may have been set from the editorial
        // section if this is a Freshly Pressed post)
        if (!post.hasPrimaryTag()) {
            post.setPrimaryTag(mostPopularTag);
        }
        post.setSecondaryTag(nextMostPopularTag);
    }

    // json "date_range" tells the the range of dates in the response - note that freshly-pressed
    // uses "newest" and "oldest" but other endpoints use "after" and "before"
    public static class ReaderDateRange {
        public String before;  // newest
        public String after;   // oldest
    }
    public ReaderDateRange getDateRange() {
        ReaderDateRange dateRange = new ReaderDateRange();

        JsonObject jsonDateRange = getRootElement().getAsJsonObject().getAsJsonObject("date_range");
        if (jsonDateRange != null) {
            dateRange.before = jsonDateRange.has("before") ? GsonUtils.getString(jsonDateRange, "before") : GsonUtils.getString(jsonDateRange, "newest");
            dateRange.after = jsonDateRange.has("after") ? GsonUtils.getString(jsonDateRange, "after") : GsonUtils.getString(jsonDateRange, "oldest");
        }

        return dateRange;
    }

}
