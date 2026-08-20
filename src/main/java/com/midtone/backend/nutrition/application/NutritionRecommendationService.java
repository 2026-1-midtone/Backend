package com.midtone.backend.nutrition.application;

import com.midtone.backend.global.user.CurrentUserIdProvider;
import com.midtone.backend.nutrition.domain.NutrientCode;
import com.midtone.backend.nutrition.domain.NutritionProduct;
import com.midtone.backend.nutrition.domain.NutritionProductFunction;
import com.midtone.backend.nutrition.domain.NutritionProductFunctionRepository;
import com.midtone.backend.nutrition.domain.NutritionProductRepository;
import com.midtone.backend.nutrition.domain.UserNutrientNeedRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NutritionRecommendationService {
    private final NutritionProductRepository productRepository;
    private final NutritionProductFunctionRepository functionRepository;
    private final UserNutrientNeedRepository needRepository;
    private final CurrentUserIdProvider currentUserIdProvider;

    public NutritionRecommendationService(NutritionProductRepository productRepository,
            NutritionProductFunctionRepository functionRepository, UserNutrientNeedRepository needRepository,
            CurrentUserIdProvider currentUserIdProvider) {
        this.productRepository = productRepository; this.functionRepository = functionRepository;
        this.needRepository = needRepository; this.currentUserIdProvider = currentUserIdProvider;
    }

    @Transactional(readOnly = true)
    public NutritionRecommendationResponse getRecommendations() {
        return getRecommendations(currentUserIdProvider.getCurrentUserId());
    }

    @Transactional(readOnly = true)
    public NutritionRecommendationResponse getRecommendations(long userId) {
        Set<NutrientCode> needs = needRepository.findAllByUserIdOrderByNutrientCodeAsc(userId).stream()
                .map(need -> need.getNutrientCode()).collect(Collectors.toSet());
        if (needs.isEmpty()) return new NutritionRecommendationResponse(List.of());
        List<NutritionProduct> products = productRepository.findAllByOrderByIdAsc();
        if (products.isEmpty()) return new NutritionRecommendationResponse(List.of());
        List<Long> ids = products.stream().map(NutritionProduct::getId).toList();
        Map<Long, List<NutritionProductFunction>> functions = functionRepository
                .findAllByProductIdInOrderByProductIdAscSortOrderAsc(ids).stream()
                .collect(Collectors.groupingBy(NutritionProductFunction::getProductId));

        List<NutritionRecommendationResponse.Recommendation> recommendations = products.stream()
                .map(product -> recommendation(product, functions.getOrDefault(product.getId(), List.of()), needs))
                .filter(item -> !item.matchedNutrients().isEmpty())
                .sorted(Comparator.comparingInt((NutritionRecommendationResponse.Recommendation item) ->
                                item.matchedNutrients().size()).reversed()
                        .thenComparingLong(NutritionRecommendationResponse.Recommendation::productId))
                .toList();
        return new NutritionRecommendationResponse(recommendations);
    }

    @Transactional(readOnly = true)
    public NutritionProductCatalogResponse getProducts() {
        List<NutritionProduct> products = productRepository.findAllByOrderByIdAsc();
        if (products.isEmpty()) return new NutritionProductCatalogResponse(List.of());
        Map<Long, List<NutritionProductFunction>> functions = functionRepository
                .findAllByProductIdInOrderByProductIdAscSortOrderAsc(products.stream().map(NutritionProduct::getId).toList())
                .stream().collect(Collectors.groupingBy(NutritionProductFunction::getProductId));
        return new NutritionProductCatalogResponse(products.stream().map(product ->
                new NutritionProductCatalogResponse.Product(product.getId(), product.getCode(), product.getName(),
                        product.getEnglishName(), product.getImageUrl(), product.getProductUrl(),
                        functionInfos(functions.getOrDefault(product.getId(), List.of())),
                        product.getDisclaimer())).toList());
    }

    private NutritionRecommendationResponse.Recommendation recommendation(NutritionProduct product,
            List<NutritionProductFunction> functions, Set<NutrientCode> needs) {
        List<String> matched = functions.stream().map(NutritionProductFunction::getNutrientCode)
                .filter(code -> code != null && needs.contains(code)).distinct().map(Enum::name).sorted().toList();
        List<NutritionRecommendationResponse.FunctionInfo> functionInfos = functionInfos(functions);
        return new NutritionRecommendationResponse.Recommendation(product.getId(), product.getCode(), product.getName(),
                product.getEnglishName(), product.getImageUrl(), product.getProductUrl(), matched, functionInfos,
                product.getDisclaimer());
    }

    private List<NutritionRecommendationResponse.FunctionInfo> functionInfos(List<NutritionProductFunction> functions) {
        return functions.stream().map(function -> new NutritionRecommendationResponse.FunctionInfo(function.getIngredient(),
                function.getNutrientCode() == null ? null : function.getNutrientCode().name(),
                function.getFunctionInfo())).toList();
    }
}
