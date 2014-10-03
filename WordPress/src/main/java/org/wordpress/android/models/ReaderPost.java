package org.wordpress.android.models;

import android.text.TextUtils;

import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.PhotonUtils;
import org.wordpress.android.util.StringUtils;

public class ReaderPost {
    private String pseudoId;
    public long postId;
    public long blogId;
    public long authorId;

    private String title;
    private String text;
    private String excerpt;
    private String authorName;
    private String blogName;
    private String blogUrl;
    private String postAvatar;

    private String primaryTag;    // most popular tag on this post based on usage in blog
    private String secondaryTag;  // second most popular tag on this post based on usage in blog

    public long timestamp;        // used for sorting
    private String published;

    private String url;
    private String featuredImage;
    private String featuredVideo;

    public int numReplies;        // includes comments, trackbacks & pingbacks
    public int numLikes;

    public boolean isLikedByCurrentUser;
    public boolean isFollowedByCurrentUser;
    public boolean isRebloggedByCurrentUser;
    public boolean isCommentsOpen;
    public boolean isExternal;
    public boolean isPrivate;
    public boolean isVideoPress;
    public boolean isJetpack;

    public boolean isLikesEnabled;
    public boolean isSharingEnabled;    // currently unused

    private String attachmentsJson;

    // --------------------------------------------------------------------------------------------

    public String getAuthorName() {
        return StringUtils.notNullStr(authorName);
    }
    public void setAuthorName(String authorName) {
        this.authorName = StringUtils.notNullStr(authorName);
    }

    public String getTitle() {
        return StringUtils.notNullStr(title);
    }
    public void setTitle(String title) {
        this.title = StringUtils.notNullStr(title);
    }

    public String getText() {
        return StringUtils.notNullStr(text);
    }
    public void setText(String text) {
        this.text = StringUtils.notNullStr(text);
    }

    public String getExcerpt() {
        return StringUtils.notNullStr(excerpt);
    }
    public void setExcerpt(String excerpt) {
        this.excerpt = StringUtils.notNullStr(excerpt);
    }

    public String getUrl() {
        return StringUtils.notNullStr(url);
    }
    public void setUrl(String url) {
        this.url = StringUtils.notNullStr(url);
    }

    public String getFeaturedImage() {
        return StringUtils.notNullStr(featuredImage);
    }
    public void setFeaturedImage(String featuredImage) {
        this.featuredImage = StringUtils.notNullStr(featuredImage);
    }

    public String getFeaturedVideo() {
        return StringUtils.notNullStr(featuredVideo);
    }
    public void setFeaturedVideo(String featuredVideo) {
        this.featuredVideo = StringUtils.notNullStr(featuredVideo);
    }

    public String getBlogName() {
        return StringUtils.notNullStr(blogName);
    }
    public void setBlogName(String blogName) {
        this.blogName = StringUtils.notNullStr(blogName);
    }

    public String getBlogUrl() {
        return StringUtils.notNullStr(blogUrl);
    }
    public void setBlogUrl(String blogUrl) {
        this.blogUrl = StringUtils.notNullStr(blogUrl);
    }

    public String getPostAvatar() {
        return StringUtils.notNullStr(postAvatar);
    }
    public void setPostAvatar(String postAvatar) {
        this.postAvatar = StringUtils.notNullStr(postAvatar);
    }

    public String getPseudoId() {
        return StringUtils.notNullStr(pseudoId);
    }
    public void setPseudoId(String pseudoId) {
        this.pseudoId = StringUtils.notNullStr(pseudoId);
    }

    public String getPublished() {
        return StringUtils.notNullStr(published);
    }
    public void setPublished(String published) {
        this.published = StringUtils.notNullStr(published);
    }

    public String getPrimaryTag() {
        return StringUtils.notNullStr(primaryTag);
    }
    public void setPrimaryTag(String tagName) {
        // this is a bit of a hack to avoid setting the primary tag to one of the default
        // tag names ("Freshly Pressed", etc.)
        if (!ReaderTag.isDefaultTagName(tagName)) {
            this.primaryTag = StringUtils.notNullStr(tagName);
        }
    }
    public boolean hasPrimaryTag() {
        return !TextUtils.isEmpty(primaryTag);
    }

    public String getSecondaryTag() {
        return StringUtils.notNullStr(secondaryTag);
    }
    public void setSecondaryTag(String tagName) {
        if (!ReaderTag.isDefaultTagName(tagName)) {
            this.secondaryTag = StringUtils.notNullStr(tagName);
        }
    }

    /*
     * attachments are stored as the actual JSON to avoid having a separate table for
     * them, may need to revisit this if/when attachments become more important
     */
    public String getAttachmentsJson() {
        return StringUtils.notNullStr(attachmentsJson);
    }
    public void setAttachmentsJson(String json) {
        attachmentsJson = StringUtils.notNullStr(json);
    }


    public boolean hasText() {
        return !TextUtils.isEmpty(text);
    }

    public boolean hasExcerpt() {
        return !TextUtils.isEmpty(excerpt);
    }

    public boolean hasFeaturedImage() {
        return !TextUtils.isEmpty(featuredImage);
    }

    public boolean hasFeaturedVideo() {
        return !TextUtils.isEmpty(featuredVideo);
    }

    public boolean hasPostAvatar() {
        return !TextUtils.isEmpty(postAvatar);
    }

    public boolean hasBlogName() {
        return !TextUtils.isEmpty(blogName);
    }

    public boolean hasAuthorName() {
        return !TextUtils.isEmpty(authorName);
    }

    public boolean hasTitle() {
        return !TextUtils.isEmpty(title);
    }

    public boolean hasBlogUrl() {
        return !TextUtils.isEmpty(blogUrl);
    }

    /*
     * only public wp posts can be reblogged
     */
    public boolean canReblog() {
        return !isExternal && !isPrivate;
    }

    /*
     * returns true if this post is from a WordPress blog
     */
    public boolean isWP() {
        return !isExternal;
    }

    /****
     * the following are transient variables - not stored in the db or returned in the json - whose
     * sole purpose is to cache commonly-used values for the post that speeds up using them inside
     * adapters
     ****/

    /*
     * returns the featured image url as a photon url set to the passed width/height
     */
    private transient String featuredImageForDisplay;
    public String getFeaturedImageForDisplay(int width, int height) {
        if (featuredImageForDisplay == null) {
            if (!hasFeaturedImage()) {
                featuredImageForDisplay = "";
            } else {
                featuredImageForDisplay = ReaderUtils.getResizedImageUrl(featuredImage, width, height, isPrivate);
            }
        }
        return featuredImageForDisplay;
    }

    /*
     * returns the avatar url as a photon url set to the passed size
     */
    private transient String avatarForDisplay;
    public String getPostAvatarForDisplay(int avatarSize) {
        if (avatarForDisplay == null) {
            if (!hasPostAvatar()) {
                return "";
            }
            avatarForDisplay = PhotonUtils.fixAvatar(postAvatar, avatarSize);
        }
        return avatarForDisplay;
    }

    /*
     * converts iso8601 published date to an actual java date
     */
    private transient java.util.Date dtPublished;
    public java.util.Date getDatePublished() {
        if (dtPublished == null) {
            dtPublished = DateTimeUtils.iso8601ToJavaDate(published);
        }
        return dtPublished;
    }

    /*
     * determine which tag to display for this post
     *  - no tag if this is a private blog or there is no primary tag for this post
     *  - primary tag, unless it's the same as the currently selected tag
     *  - secondary tag if primary tag is the same as the currently selected tag
     */
    private transient String tagForDisplay;
    public String getTagForDisplay(final String currentTagName) {
        if (tagForDisplay == null) {
            if (!isPrivate && hasPrimaryTag()) {
                if (getPrimaryTag().equalsIgnoreCase(currentTagName)) {
                    tagForDisplay = getSecondaryTag();
                } else {
                    tagForDisplay = getPrimaryTag();
                }
            } else {
                tagForDisplay = "";
            }
        }
        return tagForDisplay;
    }

    /*
     * used when a unique numeric id is required by an adapter (when hasStableIds() = true)
     */
    private transient long stableId;
    public long getStableId() {
        if (stableId == 0) {
            stableId = (pseudoId != null ? pseudoId.hashCode() : 0);
        }
        return stableId;
    }

}