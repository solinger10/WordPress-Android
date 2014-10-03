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
import org.wordpress.android.util.HtmlUtils;

import java.text.BreakIterator;
import java.util.Map;
import java.util.Set;

public class ReaderPostParser {

    private static final String EMPTY_STRING = "";

    public static ReaderPostList parsePostList(final String jsonString) {
        ReaderPostList posts = new ReaderPostList();
        if (TextUtils.isEmpty(jsonString)) {
            return posts;
        }

        JsonElement rootElem = new JsonParser().parse(jsonString);
        JsonArray jsonPostArray = rootElem.getAsJsonObject().getAsJsonArray("posts");
        for (JsonElement jsonPost: jsonPostArray) {
            ReaderPost post = parsePost(jsonPost);
            posts.add(post);
        }

        return posts;
    }

    private static ReaderPost parsePost(JsonElement jsonPost) {
        ReaderPost post = new ReaderPost();
        JsonObject json = jsonPost.getAsJsonObject();

        post.postId = getLong(json, "ID");
        post.blogId = getLong(json, "site_ID");

        if (json.has("pseudo_ID")) {
            post.setPseudoId(getString(json, "pseudo_ID"));  // read/ endpoint
        } else {
            post.setPseudoId(getString(json, "global_ID"));  // sites/ endpoint
        }

        post.setText(getString(json, "text"));
        post.setExcerpt(getStringStripHtml(json, "excerpt"));
        post.setTitle(getStringStripHtml(json, "title"));
        post.setUrl(getString(json, "URL"));
        post.setBlogUrl(getString(json, "site_URL"));

        post.numReplies = getInt(json, "comment_count");
        post.numLikes = getInt(json, "like_count");

        post.isLikedByCurrentUser = getBool(json, "i_like");
        post.isFollowedByCurrentUser = getBool(json, "is_following");
        post.isRebloggedByCurrentUser = getBool(json, "is_reblogged");
        post.isCommentsOpen = getBool(json, "comments_open");
        post.isExternal = getBool(json, "is_external");
        post.isPrivate = getBool(json, "site_is_private");

        post.isLikesEnabled = getBool(json, "likes_enabled");
        post.isSharingEnabled = getBool(json, "sharing_enabled");

        JsonElement elemAuthor = json.get("author");
        if (elemAuthor != null) {
            JsonObject objAuthor = elemAuthor.getAsJsonObject();
            post.authorId = getLong(objAuthor, "ID");
            post.setAuthorName(getString(objAuthor, "name"));
            post.setPostAvatar(getString(objAuthor, "avatar_URL"));
            // site_URL doesn't exist for /sites/ endpoints, so get it from the author
            if (!post.hasBlogUrl()) {
                post.setBlogUrl(getString(objAuthor, "URL"));
            }
        }

        // only freshly-pressed posts have the "editorial" section
        JsonElement elemEditorial = json.get("editorial");
        if (elemEditorial != null) {
            JsonObject objEditorial = elemEditorial.getAsJsonObject();
            post.blogId = getLong(objEditorial, "blog_id");
            post.setBlogName(getStringDecoded(objEditorial, "blog_name"));
            post.setFeaturedImage(ReaderImageScanner.getImageUrlFromFPFeaturedImageUrl(getString(objEditorial, "image")));
            post.setPrimaryTag(getString(objEditorial, "highlight_topic_title"));
            // we want freshly-pressed posts to show & store the date they were chosen rather than the day they were published
            post.setPublished(getString(objEditorial, "displayed_on"));
        } else {
            post.setFeaturedImage(getString(json, "featured_image"));
            post.setBlogName(getStringDecoded(json, "site_name"));
            post.setPublished(getString(json, "date"));
        }

        // the date a post was liked is only returned by the read/liked/ endpoint - if this exists,
        // set it as the timestamp so posts are sorted by the date they were liked rather than the
        // date they were published (the timestamp is used to sort posts when querying)
        String likeDate = getString(json, "date_liked");
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
            String mediaUrl = getString(objMedia, "uri");
            if (!TextUtils.isEmpty(mediaUrl)) {
                String type = getString(objMedia, "type");
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
        JsonObject objSite = getChild(json, "meta/data/site");
        if (objSite != null) {
            post.blogId = getLong(objSite, "ID");
            post.setBlogName(getString(objSite, "name"));
            post.setBlogUrl(getString(objSite, "URL"));
            post.isPrivate = getBool(objSite, "is_private");
            // TODO: as of 29-Sept-2014, this is broken - endpoint returns false when it should be true
            post.isJetpack = getBool(objSite, "jetpack");
        }

        return post;
    }

    /*
     * extracts a title from a post's excerpt - used when the post has no title
     */
    private static String extractTitle(final String excerpt, int maxLen) {
        if (TextUtils.isEmpty(excerpt))
            return null;

        if (excerpt.length() < maxLen)
            return excerpt.trim();

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
            if (totalLen >= maxLen)
                break;
            start = end;
            end = wordIterator.next();
        }

        if (totalLen==0)
            return null;
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
            int postCount = getInt(objTag, "post_count");
            if (postCount > popularCount) {
                nextMostPopularTag = mostPopularTag;
                mostPopularTag = getString(objTag, "name");
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

    /**
     * GSON helper routines
     */
    private static String getString(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsString(): EMPTY_STRING);
    }
    private static String getStringDecoded(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? HtmlUtils.fastUnescapeHtml(element.getAsString()) : EMPTY_STRING);
    }
    private static String getStringStripHtml(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? HtmlUtils.fastStripHtml(element.getAsString()) : EMPTY_STRING);
    }

    private static long getLong(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsLong(): 0);
    }

    private static int getInt(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsInt(): 0);
    }

    private static boolean getBool(JsonObject json, String name) {
        JsonElement element = json.get(name);
        return (element != null ? element.getAsBoolean(): false);
    }

    public static JsonObject getChild(final JsonObject jsonParent, final String query) {
        if (jsonParent == null || TextUtils.isEmpty(query))
            return null;
        String[] names = query.split("/");
        JsonObject jsonChild = null;
        for (int i = 0; i < names.length; i++) {
            if (jsonChild == null) {
                jsonChild = jsonParent.getAsJsonObject(names[i]);
            } else {
                jsonChild = jsonChild.getAsJsonObject(names[i]);
            }
            if (jsonChild == null)
                return null;
        }
        return jsonChild;
    }
}
