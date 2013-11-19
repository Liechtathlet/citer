/**
 * 
 */
package ch.zhaw.mdp.lhb.citr.rest.impl;

import ch.zhaw.mdp.lhb.citr.dto.GroupDTO;
import ch.zhaw.mdp.lhb.citr.dto.GroupFactory;
import ch.zhaw.mdp.lhb.citr.dto.UserDTO;
import ch.zhaw.mdp.lhb.citr.dto.UserFactory;
import ch.zhaw.mdp.lhb.citr.jpa.entity.GroupDVO;
import ch.zhaw.mdp.lhb.citr.jpa.entity.UserDVO;
import ch.zhaw.mdp.lhb.citr.jpa.service.IDBUserService;
import ch.zhaw.mdp.lhb.citr.response.ResponseObject;
import ch.zhaw.mdp.lhb.citr.rest.IRUserServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

/**
 * @author Daniel Brun
 * 
 *         Implementation of the Service-Interface {@link IRUserServices}.
 */
@Component
@Path("/user")
@Scope("singleton")
public class UserServiceRestImpl implements IRUserServices {

	private static final Logger LOG = LoggerFactory
			.getLogger(UserServiceRestImpl.class);

	@Autowired
	private IDBUserService userService;

	@Autowired
	private ReloadableResourceBundleMessageSource messageSource;

	/*
	 * (non-Javadoc)
	 * 
	 * @see ch.zhaw.mdp.lhb.citr.rest.IRUserServices#getUser(java.lang.String)
	 */
	@GET
	@Override
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured("ROLE_USER")
	@Path("/{id}/login")
	public ResponseObject<UserDTO> loginUser(@PathParam("id") String anOpenId) {
		UserDTO resUser = null;
		boolean successfull = false;
		String msg = "";

		Authentication auth = SecurityContextHolder.getContext()
				.getAuthentication();
		
		if (auth != null && auth.getName().equals(anOpenId)) {
			UserDVO user = new UserDVO();
			user.setOpenId(anOpenId);

			resUser = UserFactory.createUserDTO(userService.findPerson(user));

			if (resUser == null) {
				LOG.info("Authentication for user: + " + anOpenId + " failed");
				msg = messageSource.getMessage("msg.user.auth.failed", null,
						null);
			} else {
				LOG.info("Authentication for user: + " + anOpenId
						+ " was successfull");
				msg = messageSource.getMessage("msg.user.auth.succ",
						new String[] { resUser.getUsername() }, null);
				successfull = true;
			}
		} else {
			msg = messageSource.getMessage("msg.user.noPermission", null, null);
		}

		return new ResponseObject<UserDTO>(resUser, successfull, msg);
	}

	@Override
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured("ROLE_USER")
	@Path("/register")
	public ResponseObject<Boolean> registerUser(UserDTO aUser) {
		UserDVO user = UserFactory.createUserDVO(aUser);
		Boolean result = Boolean.FALSE;
		String msg = "";

		if (user == null) {
			throw new IllegalArgumentException(
					"The argument aUser must not be null!");
		}

		if (userService.findPerson(user) == null) {
			if (aUser.getUsername() != null && aUser.getOpenId() != null) {
				if (userService.save(user)) {
					result = Boolean.TRUE;
				} else {
					msg = messageSource.getMessage("msg.user.create.fail",
							null, null);
				}
			} else {
				msg = messageSource.getMessage("msg.user.data.invalid", null,
						null);
			}
		} else {
			msg = messageSource
					.getMessage("msg.user.alreadyExists", null, null);
		}

		return new ResponseObject<Boolean>(result, result.booleanValue(), msg);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured("ROLE_USER")
	@Path("/groups")
	public ResponseObject<List<GroupDTO>> getGroups() {

		boolean successfull = true;
		//FIXME: De Text wo zrug geh wird, wird 1:1 im GUI azeigt, wenn nüt sell azeigt werde, würi sege eifacht null zrug geh.
		//BTW: getGroups isch eigentli s gliche wie getGroupSubscriptions
		//Würd evtl. e neue UserFactory, bzw. en Helper oder so um de loggedInUser überzcho, sust hesch i de Factory verschideni Theme gmischt...
		//Evtl. mümer uf de GroupDTO no es Flag mache, wo azeigt wie de "Request" status isch -> approved, pending, ....
		String message = "ok";

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		UserDVO user = new UserDVO();
		user.setOpenId(auth.getName());

		user = userService.findPerson(user);

		LOG.info(String.format("/user/groups: Got user: id %d, openId %s", user.getId(), user.getOpenId()));
		List<GroupDVO> groupDVOs = user.getGroups();
		List<GroupDTO> groupDTOs = null;
		try {
			groupDTOs = GroupFactory.createGroups(groupDVOs);
		} catch (Exception e) {
			LOG.error(e.getMessage());
		}
		LOG.info("Got groups");

		if (groupDTOs == null) {
			successfull = false;
			message = messageSource.getMessage("msg.user.getGroups.fail", null, null);
		}

		return new ResponseObject<List<GroupDTO>>(groupDTOs, successfull, message);
	}
}
