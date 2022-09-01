/**
 *  This work is based on original code developed and copyrighted by TNO 2020. 
 *  Subsequent contributions are licensed to you by the developers of such code and are
 *  made available to the Project under one or several contributor license agreements.
 *
 *  This work is licensed to you under the Apache License, Version 2.0.
 *  You may obtain a copy of the license at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Contributors:
 *      TNO         - Initial implementation
 *  Manager:
 *      TNO
 */

package nl.tno.esdl.esdldrive.cdo;

import java.util.List;

import com.ibm.websphere.security.openidconnect.token.IdToken;

import nl.tno.esdl.esdldrive.cdo.UserInfo.InvalidLoginInformationException;

/**
 * Defines access control mechanisms for Mondaine CDO
 * This is based on the Keycloack user_group claim, the groups a user belongs to.
 * 
 * @author werkmane
 *
 */
public class AccessController {
	
	public enum FolderType {
		Organizations,
		Projects,
		Users
	}
	
	public static UserInfo userInfoFromToken(IdToken jwtToken) throws InvalidLoginInformationException {
		return new UserInfo(jwtToken);
	}
	
	public static boolean isWritable(String path, List<String> accessGroups) {
		
		return false;
	}
	
	public static HubPermission getPermission(String path, List<String> accessGroups) {
		for (String group: accessGroups) {
			if (path.startsWith(group)) {
				return new HubPermission(path, group, true, true);
			} else if (group.startsWith(path)) {
				// upper folders are not writable
				return new HubPermission(path, group, false, true);
				
			}
		}
		return new HubPermission(path, null, false, false);
	}
	
	
	
	public static boolean canAccess(String path, List<String> accessGroups) {
		// path = /Users/ewoud.werkman@tno.nl/Vesta resultaten/vesta_output_warmtekeuze_per_buurt.esdl
		// path = /Projects/Mondaine/Gooi en Vechtstreek/Test_energy_system.esdl
		// path = /Organisations/TNO/MCS/Veluwe als PV park.esdl
		
		for (String group: accessGroups) {
			System.out.println("Checking " + path + " against " + group);
			if (path.startsWith(group) || group.startsWith(path)) {
				return true;
			}
		}
		
		return false;
	}

}
