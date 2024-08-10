package login.tikichat.domain.host.repository.impl;

import login.tikichat.domain.host.model.HostFollowStatus;
import login.tikichat.domain.host.model.QHostSubscription;
import login.tikichat.domain.host.repository.CustomHostFollowStatusRepository;
import org.springframework.data.jpa.repository.support.QuerydslRepositorySupport;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class HostFollowStatusRepositoryImpl extends QuerydslRepositorySupport implements CustomHostFollowStatusRepository {
    public HostFollowStatusRepositoryImpl() {
        super(HostFollowStatus.class);
    }

    @Override
    public boolean existsByHostIdAndFollowerUserId(Long hostId, Long userId) {

        final var hostSubscriptionQ = QHostSubscription.hostSubscription;
        final var query = super.from(hostSubscriptionQ);

        return query.where(hostSubscriptionQ.host.id.eq(hostId)
                        .and(hostSubscriptionQ.follower.user.id.eq(userId)))
                .fetchCount() > 0;
    }

    @Override
    public List<HostFollowStatus> findByFollowerUserId(Long userId) {
        final var hostSubscriptionQ = QHostSubscription.hostSubscription;
        final var query = super.from(hostSubscriptionQ);

        return query.where(hostSubscriptionQ.follower.user.id.eq(userId))
                .fetch();
    }

    @Override
    public List<HostFollowStatus> findByHostUserId(Long userId) {
        final var hostSubscriptionQ = QHostSubscription.hostSubscription;
        final var query = super.from(hostSubscriptionQ);

        return query.where(hostSubscriptionQ.host.user.id.eq(userId))
                .fetch();
    }
}
