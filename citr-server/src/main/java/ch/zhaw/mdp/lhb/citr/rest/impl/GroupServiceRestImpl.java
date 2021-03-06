/**
 *
 */
package ch.zhaw.mdp.lhb.citr.rest.impl;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.hibernate.cfg.NotYetImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ch.zhaw.mdp.lhb.citr.Logging.LoggingFactory;
import ch.zhaw.mdp.lhb.citr.Logging.LoggingStrategy;
import ch.zhaw.mdp.lhb.citr.dto.GroupDTO;
import ch.zhaw.mdp.lhb.citr.dto.GroupFactory;
import ch.zhaw.mdp.lhb.citr.dto.MessageDTO;
import ch.zhaw.mdp.lhb.citr.dto.MessageFactory;
import ch.zhaw.mdp.lhb.citr.dto.SubscriptionDTO;
import ch.zhaw.mdp.lhb.citr.dto.SubscriptionFactory;
import ch.zhaw.mdp.lhb.citr.jpa.entity.GroupDVO;
import ch.zhaw.mdp.lhb.citr.jpa.entity.MessageDVO;
import ch.zhaw.mdp.lhb.citr.jpa.entity.SubscriptionDVO;
import ch.zhaw.mdp.lhb.citr.jpa.entity.UserDVO;
import ch.zhaw.mdp.lhb.citr.jpa.service.GroupRepository;
import ch.zhaw.mdp.lhb.citr.jpa.service.MessageRepository;
import ch.zhaw.mdp.lhb.citr.jpa.service.SubscriptionRepository;
import ch.zhaw.mdp.lhb.citr.jpa.service.UserRepository;
import ch.zhaw.mdp.lhb.citr.response.ResponseObject;
import ch.zhaw.mdp.lhb.citr.rest.GroupServices;

/**
 * @author Daniel Brun
 *         <p/>
 *         Implementation of the Service-Interface {@link IRGroupServices}.
 */
@Component
@Path("/group")
@Scope("singleton")
public class GroupServiceRestImpl implements GroupServices {

    private static final LoggingStrategy LOG = LoggingFactory.get();

    @Autowired
    private GroupRepository groupRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private SubscriptionRepository subscriptionRepo;

    @Autowired
    private MessageRepository messageRepo;

    @Autowired
    private ReloadableResourceBundleMessageSource messageSource;

    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("/create")
    public ResponseObject<Boolean> createGroup(GroupDTO aGroup) {
	if (aGroup == null) {
	    throw new IllegalArgumentException(
		    "The argument aGroup must not be null!");
	}

	Boolean result = Boolean.FALSE;
	String msgKey = "msg.argument.invalid";
	String msg = null;

	if (aGroup != null) {
	    UserDVO userDVO = getCurrentUser();

	    // FIXME: Check if group with same name already exists
	    // msgKey = "msg.group.alreadyExists";

	    GroupDVO group = new GroupDVO();
	    group.setName(aGroup.getName());
	    group.setMode(aGroup.isPublicGroup() ? GroupDVO.Mode.PUBLIC
		    : GroupDVO.Mode.PRIVATE);
	    group.setOwner(userDVO);

	    if (groupRepo.create(group) > 0) {

		// Set Tags
		group.setTags(aGroup.getTags());
		groupRepo.addTagsToGroup(group);

		msg = messageSource.getMessage("msg.group.create.succ",
			new String[] { group.getName() }, null);
		result = Boolean.TRUE;
	    } else {
		msgKey = "msg.data.save.failed";
	    }
	}

	if (msg == null) {
	    msg = messageSource.getMessage(msgKey, null, null);
	}

	return new ResponseObject<Boolean>(result, result.booleanValue(), msg);
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("/list")
    public ResponseObject<List<GroupDTO>> getAllGroups() {
	List<GroupDTO> groups = GroupFactory.createGroups(groupRepo.getAll());
	return new ResponseObject<List<GroupDTO>>(groups, true, null);
    }

    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/getSubscriptions")
    public ResponseObject<List<SubscriptionDTO>> getGroupSubscriptionRequests(
	    @PathParam("groupId") int aGroupId) {

	String msgKey = "msg.argument.invalid";
	Boolean succ = Boolean.FALSE;

	List<SubscriptionDTO> subscriptionDTOs = null;

	GroupDVO groupDVO = groupRepo.getById(aGroupId);

	if (groupDVO == null) {
	    msgKey = "msg.group.notFound";
	} else {
	    try {
		List<SubscriptionDVO> subscriptionDVOs = subscriptionRepo
			.getSubscriptionByGroup(groupDVO,
				SubscriptionDVO.State.OPEN);

		subscriptionDTOs = SubscriptionFactory
			.createSubscriptionDTOs(subscriptionDVOs);

		succ = Boolean.TRUE;
		msgKey = "msg.data.read.succ";
	    } catch (Exception e) {
		msgKey = "msg.subscription.req.read.fail";
		LOG.error(e.toString());
	    }
	}

	return new ResponseObject<List<SubscriptionDTO>>(subscriptionDTOs,
		succ, messageSource.getMessage(msgKey, null, null));
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("/listSubscriptions")
    public ResponseObject<List<SubscriptionDTO>> getUserSubscriptions() {
	String msgKey = "msg.subscription.read.fail";
	Boolean succ = Boolean.FALSE;

	List<SubscriptionDVO> subscriptionDVOs = getCurrentUser()
		.getSubscriptions();
	List<SubscriptionDTO> subscriptionDTOs = SubscriptionFactory
		.createSubscriptionDTOs(subscriptionDVOs);

	if (subscriptionDTOs != null) {
	    succ = Boolean.TRUE;
	    msgKey = "msg.data.read.succ";
	}

	return new ResponseObject<List<SubscriptionDTO>>(subscriptionDTOs,
		succ, messageSource.getMessage(msgKey, null, null));
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("/listOwnedGroups")
    public ResponseObject<List<GroupDTO>> getUserGroups() {
	String msgKey = "msg.user.getGroups.fail";
	Boolean succ = Boolean.FALSE;

	List<GroupDVO> groupDVOs = getCurrentUser().getCreatedGroups();
	List<GroupDTO> groupDTOs = GroupFactory.createGroups(groupDVOs);

	if (groupDTOs != null) {
	    succ = Boolean.TRUE;
	    msgKey = "msg.data.read.succ";
	}

	return new ResponseObject<List<GroupDTO>>(groupDTOs, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("/search")
    public ResponseObject<List<GroupDTO>> searchGroup(GroupDTO aGroup) {
	throw new NotYetImplementedException(
		"Diese Methode wurde noch nicht implementiert");
    }

    @Override
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/subscribe")
    public ResponseObject<Boolean> subscribe(@PathParam("groupId") int aGroupId) {
	String msgKey = "msg.subscription.create.fail";
	Boolean succ = Boolean.FALSE;

	GroupDVO groupDVO = groupRepo.getById(aGroupId);
	UserDVO userDVO = getCurrentUser();

	if (groupDVO == null) {
	    msgKey = "msg.group.notFound";
	} else if (subscriptionRepo.hasUserGroupSubscription(userDVO, groupDVO)) {
	    msgKey = "msg.subscription.alreadyExists";
	} else {
	    SubscriptionDVO subscriptionDVO = new SubscriptionDVO();

	    subscriptionDVO.setUserId(userDVO.getId());
	    subscriptionDVO.setUser(userDVO);
	    subscriptionDVO.setGroupId(groupDVO.getId());
	    subscriptionDVO.setGroup(groupDVO);

	    if (groupDVO.getMode() == GroupDVO.Mode.PUBLIC) {
		subscriptionDVO.setState(SubscriptionDVO.State.APPROVED);

		if (subscriptionRepo.save(subscriptionDVO)) {
		    succ = Boolean.TRUE;
		    msgKey = "msg.subscription.inscription.succ";
		}
	    } else {
		subscriptionDVO.setState(SubscriptionDVO.State.OPEN);

		if (subscriptionRepo.save(subscriptionDVO)) {
		    succ = Boolean.TRUE;
		    msgKey = "msg.subscription.inscription.pending";
		}
	    }
	}
	return new ResponseObject<Boolean>(succ, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    @GET
    @Override
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/getNewestMessage")
    public ResponseObject<MessageDTO> getNewestMessage(
	    @PathParam("groupId") int aGroupId) {

	String msgKey = "msg.no.data";
	Boolean succ = Boolean.FALSE;

	List<MessageDVO> messageDVOs = messageRepo.getMessagesByGroup(aGroupId,
		1);
	MessageDTO messageDTO = null;

	if (messageDVOs != null && messageDVOs.size() >= 1) {
	    messageDTO = MessageFactory.createMessageDTO(messageDVOs.get(0));

	    msgKey = "msg.data.read.succ";
	    succ = Boolean.TRUE;
	}

	return new ResponseObject<MessageDTO>(messageDTO, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    @DELETE
    @Override
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/delete")
    public ResponseObject<Boolean> deleteGroup(
	    @PathParam("groupId") int aGroupId) {
	String msgKey = "msg.group.delete.fail";
	Boolean succ = Boolean.FALSE;

	if (groupRepo.remove(aGroupId)) {
	    succ = Boolean.TRUE;
	    msgKey = "msg.group.delete.succ";
	}

	return new ResponseObject<Boolean>(succ, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/user/{userId}/acceptSubscription")
    public ResponseObject<Boolean> acceptSubscription(
	    @PathParam("groupId") int aGroupId, @PathParam("userId") int aUserId) {
	String msgKey = "msg.subscription.req.accept.fail";
	Boolean succ = Boolean.FALSE;

	GroupDVO groupDVO = groupRepo.getById(aGroupId);
	UserDVO userDVO = userRepo.getById(aUserId);

	if (groupDVO == null) {
	    msgKey = "msg.group.notFound";
	} else if (userDVO == null) {
	    msgKey = "msg.user.notFound";
	} else if (!subscriptionRepo
		.hasUserGroupSubscription(userDVO, groupDVO)) {
	    msgKey = "msg.subscription.req.notfound";
	} else {
	    SubscriptionDVO subscriptionDVO = subscriptionRepo.getSubscription(
		    userDVO, groupDVO);

	    if (subscriptionDVO.getState() == SubscriptionDVO.State.APPROVED) {
		msgKey = "msg.subscription.req.already.app";
	    }

	    subscriptionRepo.updateState(subscriptionDVO,
		    SubscriptionDVO.State.APPROVED);

	    succ = Boolean.TRUE;
	    msgKey = "msg.subscription.req.app";
	}

	return new ResponseObject<Boolean>(succ, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    @Override
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/user/{userId}/declineSubscription")
    public ResponseObject<Boolean> declineSubscription(
	    @PathParam("groupId") int aGroupId, @PathParam("userId") int aUserId) {
	String msgKey = "msg.subscription.req.dec.fail";
	Boolean succ = Boolean.FALSE;

	GroupDVO groupDVO = groupRepo.getById(aGroupId);
	UserDVO userDVO = userRepo.getById(aUserId);

	if (groupDVO == null) {
	    msgKey = "msg.group.notFound";
	} else if (userDVO == null) {
	    msgKey = "msg.user.notFound";
	} else if (!subscriptionRepo
		.hasUserGroupSubscription(userDVO, groupDVO)) {
	    msgKey = "msg.subscription.req.notfound";
	} else {

	    SubscriptionDVO subscriptionDVO = subscriptionRepo.getSubscription(
		    userDVO, groupDVO);

	    if (subscriptionDVO.getState() == SubscriptionDVO.State.APPROVED) {
		msgKey = "msg.subscription.req.already.app";
	    }

	    subscriptionDVO.setState(SubscriptionDVO.State.APPROVED);

	    if (subscriptionRepo.remove(subscriptionDVO)) {
		succ = Boolean.TRUE;
		msgKey = "msg.subscription.req.dec";
	    }
	}

	return new ResponseObject<Boolean>(succ, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    @Override
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Secured("ROLE_USER")
    @Path("{groupId}/messages")
    public ResponseObject<List<MessageDTO>> getMessages(
	    @PathParam("groupId") int aGroupId,
	    @DefaultValue("0") @QueryParam("messageBefore") int messageBefore,
	    @DefaultValue("10") @QueryParam("count") int count) {

	String msgKey = "msg.no.data";
	Boolean succ = Boolean.FALSE;

	List<MessageDTO> messageDTOs = null;
	List<MessageDVO> messageDVOs = null;

	if (messageBefore == 0) {
	    messageDVOs = messageRepo.getMessagesByGroup(aGroupId, count);
	} else {
	    MessageDVO messageDVO = messageRepo.getMessageById(messageBefore);

	    if (messageDVO != null) {
		messageDVOs = messageRepo.getMessagesByGroup(aGroupId, count,
			messageDVO.getSendDate());
	    }
	}

	if (messageDVOs != null) {
	    messageDTOs = MessageFactory.createMessageDTOs(messageDVOs);

	    succ = Boolean.TRUE;
	    msgKey = "msg.data.read.succ";
	} else {
	    messageDTOs = new ArrayList<MessageDTO>();
	}

	return new ResponseObject<List<MessageDTO>>(messageDTOs, succ,
		messageSource.getMessage(msgKey, null, null));
    }

    /**
     * @return the current user.
     */
    private UserDVO getCurrentUser() {
	Authentication auth = SecurityContextHolder.getContext()
		.getAuthentication();
	return userRepo.getByOpenId(auth.getName());
    }
}
