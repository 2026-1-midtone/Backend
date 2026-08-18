package com.midtone.backend.nutrition.application;

import com.midtone.backend.global.time.DateTimeDefaults;
import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nutrition.domain.NutritionContent;
import com.midtone.backend.nutrition.domain.NutritionContentRepository;
import com.midtone.backend.nutrition.domain.NutritionContentType;
import com.midtone.backend.nutrition.domain.NutritionFavorite;
import com.midtone.backend.nutrition.domain.NutritionFavoriteRepository;
import com.midtone.backend.nutrition.domain.NutritionTimingTag;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutritionContentService {
    private static final int PAGE_SIZE = 20;
    private final NutritionContentRepository contentRepository; private final NutritionFavoriteRepository favoriteRepository;
    private final CurrentUserIdProvider userProvider; private final NutritionTimingResolver timingResolver;
    public NutritionContentService(NutritionContentRepository contentRepository,
            NutritionFavoriteRepository favoriteRepository, CurrentUserIdProvider userProvider,
            NutritionTimingResolver timingResolver) {
        this.contentRepository = contentRepository; this.favoriteRepository = favoriteRepository;
        this.userProvider = userProvider; this.timingResolver = timingResolver;
    }

    @Transactional(readOnly = true)
    public NutritionResponses.ListResponse list(String timingTagValue, String contentTypeValue, int pageNumber) {
        validatePage(pageNumber); long userId = userProvider.getCurrentUserId();
        NutritionTimingTag timingTag = timingTagValue == null || timingTagValue.isBlank()
                ? timingResolver.resolve(userId) : parseTiming(timingTagValue);
        NutritionContentType contentType = contentTypeValue == null || contentTypeValue.isBlank()
                ? null : parseContentType(contentTypeValue);
        PageRequest pageRequest = PageRequest.of(pageNumber, PAGE_SIZE);
        Page<NutritionContent> page = contentType == null
                ? contentRepository.findAllByTimingTagOrderByCreatedAtDescIdDesc(timingTag, pageRequest)
                : contentRepository.findAllByTimingTagAndContentTypeOrderByCreatedAtDescIdDesc(timingTag, contentType, pageRequest);
        Set<Long> favorites = favoriteIds(userId, page.getContent().stream().map(NutritionContent::getId).toList());
        return new NutritionResponses.ListResponse(timingTag.name(), page.getContent().stream()
                .map(content -> summary(content, favorites.contains(content.getId()))).toList(),
                new NutritionResponses.PageInfo(pageNumber, PAGE_SIZE, page.getTotalElements()));
    }

    @Transactional(readOnly = true)
    public NutritionResponses.Detail detail(long contentId) {
        long userId = userProvider.getCurrentUserId();
        NutritionContent content = getContent(contentId);
        return new NutritionResponses.Detail(content.getId(), content.getContentType().name(), content.getTimingTag().name(),
                content.getTitle(), content.getSummary(), content.getBody(), content.getSourceUrl(), content.getDisclaimer(),
                favoriteRepository.existsByUserIdAndContentId(userId, contentId));
    }

    @Transactional
    public NutritionResponses.Favorite addFavorite(long contentId) {
        long userId = userProvider.getCurrentUserId(); getContent(contentId);
        if (favoriteRepository.existsByUserIdAndContentId(userId, contentId))
            throw new NutritionException(NutritionException.ErrorCode.ALREADY_FAVORITED);
        favoriteRepository.save(new NutritionFavorite(userId, contentId));
        return new NutritionResponses.Favorite(contentId, true);
    }

    @Transactional
    public void removeFavorite(long contentId) {
        long userId = userProvider.getCurrentUserId();
        NutritionFavorite favorite = favoriteRepository.findByUserIdAndContentId(userId, contentId)
                .orElseThrow(() -> new NutritionException(NutritionException.ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public NutritionResponses.FavoritesResponse favorites(int pageNumber) {
        validatePage(pageNumber); long userId = userProvider.getCurrentUserId();
        Page<NutritionFavorite> page = favoriteRepository.findAllByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.of(pageNumber, PAGE_SIZE));
        Map<Long, NutritionContent> contents = contentRepository.findAllById(
                page.getContent().stream().map(NutritionFavorite::getContentId).toList()).stream()
                .collect(Collectors.toMap(NutritionContent::getId, Function.identity()));
        var items = page.getContent().stream().filter(favorite -> contents.containsKey(favorite.getContentId()))
                .map(favorite -> favoriteSummary(contents.get(favorite.getContentId()), favorite)).toList();
        return new NutritionResponses.FavoritesResponse(items,
                new NutritionResponses.PageInfo(pageNumber, PAGE_SIZE, page.getTotalElements()));
    }

    private NutritionContent getContent(long id) { return contentRepository.findById(id)
            .orElseThrow(() -> new NutritionException(NutritionException.ErrorCode.CONTENT_NOT_FOUND)); }
    private Set<Long> favoriteIds(long userId, Collection<Long> ids) { return ids.isEmpty() ? Set.of()
            : favoriteRepository.findAllByUserIdAndContentIdIn(userId, ids).stream()
                    .map(NutritionFavorite::getContentId).collect(Collectors.toSet()); }
    private NutritionResponses.Summary summary(NutritionContent c, boolean favorite) { return new NutritionResponses.Summary(
            c.getId(), c.getContentType().name(), c.getTimingTag().name(), c.getTitle(), c.getSummary(), c.getThumbnailUrl(), favorite); }
    private NutritionResponses.FavoriteSummary favoriteSummary(NutritionContent c, NutritionFavorite f) {
        return new NutritionResponses.FavoriteSummary(c.getId(), c.getContentType().name(), c.getTimingTag().name(),
                c.getTitle(), c.getSummary(), c.getThumbnailUrl(),
                f.getCreatedAt().atZone(DateTimeDefaults.DEFAULT_ZONE).toOffsetDateTime()); }
    private void validatePage(int page) { if (page < 0) throw new NutritionException(NutritionException.ErrorCode.INVALID_PAGE); }
    private NutritionTimingTag parseTiming(String value) { try { return NutritionTimingTag.valueOf(value); }
        catch (IllegalArgumentException exception) { throw new NutritionException(NutritionException.ErrorCode.INVALID_TIMING_TAG); } }
    private NutritionContentType parseContentType(String value) { try { return NutritionContentType.valueOf(value); }
        catch (IllegalArgumentException exception) { throw new NutritionException(NutritionException.ErrorCode.INVALID_CONTENT_TYPE); } }
}
