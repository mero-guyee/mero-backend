package io.mero.app.domain.trip.service;

import io.mero.app.domain.budget.entity.Budget;
import io.mero.app.domain.expense.entity.Expense;
import io.mero.app.domain.expense.entity.ExpenseCategory;
import io.mero.app.domain.footprint.entity.Footprint;
import io.mero.app.domain.footprint.entity.FootprintLocation;
import io.mero.app.domain.footprint.entity.Photo;
import io.mero.app.domain.trip.entity.Trip;
import io.mero.app.domain.trip.entity.TripCoverImage;
import io.mero.app.domain.trip.entity.TripDocument;
import io.mero.app.domain.trip.entity.TripMemo;
import io.mero.app.domain.user.entity.User;
import io.mero.app.global.enums.Currency;
import io.mero.app.global.enums.DocumentMimeType;
import io.mero.app.global.enums.ImageMimeType;
import io.mero.app.global.service.StorageCleaner;
import io.mero.app.global.service.StorageService;
import io.mero.app.global.util.MessageUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("local")
@Import(TripService.class)
class TripDeleteJpaTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    TripService tripService;

    @MockitoBean
    StorageService storageService;
    @MockitoBean
    StorageCleaner storageCleaner;
    @MockitoBean
    MessageUtil messageUtil;

    private User user;
    private Trip trip;

    private void base() {
        user = User.builder().email("a@b.c").nickname("n").build();
        em.persist(user);
        trip = Trip.builder()
                .user(user).title("t")
                .startDate(LocalDate.now()).endDate(LocalDate.now())
                .countries(List.of("KR")).clientId("trip-1")
                .build();
        em.persist(trip);
    }

    private void deleteAndFlush() {
        em.flush();
        em.clear();
        tripService.deleteTrip(user.getId(), trip.getId());
        em.flush();
    }

    @Test
    @DisplayName("soft delete된 자식까지 있는 여행을 삭제해도 flush가 성공한다")
    void deleteTripWithSoftDeletedChildren() {
        base();

        TripCoverImage cover = TripCoverImage.builder()
                .trip(trip).storageKey("cover").mimeType(ImageMimeType.JPEG).build();
        em.persist(cover);
        trip.setCoverImage(cover);

        em.persist(doc(trip, "doc-live", false));
        em.persist(doc(trip, "doc-dead", true));
        em.persist(memo(trip, "memo-1"));
        em.persist(budget(trip, "budget-live", false));
        em.persist(budget(trip, "budget-dead", true));

        ExpenseCategory category = ExpenseCategory.builder()
                .user(user).name("food").isDefault(true).displayOrder(1).clientId("cat-1").build();
        em.persist(category);

        Footprint liveFp = footprint(trip, "fp-live", false);
        em.persist(liveFp);
        Footprint deadFp = footprint(trip, "fp-dead", true);
        em.persist(deadFp);

        em.persist(photo(liveFp, "p-live", 0, false));
        em.persist(photo(liveFp, "p-dead", 1, true));
        em.persist(photo(deadFp, "p-on-dead-fp", 0, false));
        em.persist(location(liveFp));
        em.persist(location(deadFp));

        em.persist(expense(trip, liveFp, category, "exp-live", false));
        em.persist(expense(trip, deadFp, category, "exp-dead", true));

        deleteAndFlush();

        em.clear();
        assertThat(em.find(Trip.class, trip.getId())).isNull();
    }

    private TripDocument doc(Trip trip, String clientId, boolean deleted) {
        TripDocument d = TripDocument.builder()
                .trip(trip).originalFileName("f.pdf").storageKey("k-" + clientId)
                .fileSize(1L).contentType(DocumentMimeType.PDF).clientId(clientId).build();
        if (deleted) d.delete();
        return d;
    }

    private TripMemo memo(Trip trip, String clientId) {
        return TripMemo.builder().trip(trip).title("t").content("c").clientId(clientId).build();
    }

    private Budget budget(Trip trip, String clientId, boolean deleted) {
        Budget b = Budget.builder()
                .trip(trip).amount(BigDecimal.TEN).currency(Currency.KRW).clientId(clientId).build();
        if (deleted) b.delete();
        return b;
    }

    private Footprint footprint(Trip trip, String clientId, boolean deleted) {
        Footprint f = Footprint.builder()
                .trip(trip).title("t").content("c").date(LocalDate.now()).clientId(clientId).build();
        if (deleted) f.delete();
        return f;
    }

    private Photo photo(Footprint fp, String clientId, int order, boolean deleted) {
        Photo p = Photo.builder()
                .footprint(fp).storageKey("k-" + clientId).mimeType(ImageMimeType.JPEG)
                .orderIndex(order).clientId(clientId).build();
        if (deleted) p.delete();
        return p;
    }

    private FootprintLocation location(Footprint fp) {
        return FootprintLocation.builder().footprint(fp).placeName("p").sortOrder(0).build();
    }

    private Expense expense(Trip trip, Footprint fp, ExpenseCategory category,
                            String clientId, boolean deleted) {
        Expense e = Expense.builder()
                .trip(trip).footprint(fp).amount(BigDecimal.ONE).currency(Currency.KRW)
                .category(category).date(LocalDate.now()).clientId(clientId).build();
        if (deleted) e.delete();
        return e;
    }
}
