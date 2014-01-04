package ch.zhaw.mdp.lhb.citr.jpa.service.impl;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.zhaw.mdp.lhb.citr.jpa.entity.GroupDVO;
import ch.zhaw.mdp.lhb.citr.jpa.service.GroupRepository;

/**
 * @author Daniel Brun
 * @author Simon Lang
 *
 * Implementation of the DB-Service interface {@link GroupRepository}
 */
@Service("groupService")
public class GroupRepositoryJpaImpl implements GroupRepository {

	private EntityManager entityManager;

	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public GroupDVO getById(int id) {
		return entityManager.find(GroupDVO.class, id);
	}

	@Override
	@Transactional(readOnly = true)
	public List<GroupDVO> getAll() {
		Query query = entityManager.createNamedQuery("Group.findAll");
		return query.getResultList();
	}

	@Override
	@Transactional(readOnly = true)
	public int create(GroupDVO group) {
		entityManager.persist(group);
		entityManager.flush();
		return group.getId();
	}

	@Override
	@Transactional(readOnly = true)
	public void remove(int groupId) throws Exception {
		Query subscriptionQuery = entityManager.createQuery("delete from SubscriptionDVO s where s.groupId = :groupId");
		Query messageQuery = entityManager.createQuery("delete from MessageDVO m where m.groupId = :groupId");
		Query tagQuery = entityManager.createQuery("delete from TagsDVO t where t.groupId = :groupId");
		Query groupQuery = entityManager.createQuery("delete from GroupDVO g where g.id = :groupId");

		subscriptionQuery.setParameter("groupId", groupId);
		messageQuery.setParameter("groupId", groupId);
		tagQuery.setParameter("groupId", groupId);
		groupQuery.setParameter("groupId", groupId);

		subscriptionQuery.executeUpdate();
		messageQuery.executeUpdate();
		tagQuery.executeUpdate();
		groupQuery.executeUpdate();
	}
}
