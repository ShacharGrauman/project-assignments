package com.grauman.amdocs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.webresources.Cache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.grauman.amdocs.dao.interfaces.IRoleDAO;
import com.grauman.amdocs.models.EmployeeException;
import com.grauman.amdocs.models.Permission;
import com.grauman.amdocs.models.Role;
import com.grauman.amdocs.models.RolePermissions;

@Service
public class RoleDAO implements IRoleDAO {
	@Autowired
	private DBManager db;

	// validation done
	@Override
	public List<RolePermissions> findAll() throws SQLException,Exception {
		List<Role> roles = new ArrayList<>();
		List<RolePermissions> rolesWithPermissions = new ArrayList<>();
		boolean catchTimeOut = false;
		int tries = 0;
		ResultSet result2 = null;
		ResultSet result = null;
		String sqlFindRoles = "select id,name from roles";
		String findRolePermissions = "select P.id,P.name"
				+ " from permissions P JOIN rolepermissions RP ON p.id=RP.role_id"
				+ " where RP.permission_id=P.id AND RP.role_id=?";

		try (Connection conn = db.getConnection()) {
			try (Statement command = conn.createStatement()) {
				do {
					try {
						result = command.executeQuery(sqlFindRoles);
						catchTimeOut = false;
					} catch (SQLTimeoutException e) {
						catchTimeOut = true;
						if (tries++ > 3)
							throw e;
					}
				} while (catchTimeOut);

				if (!result.next())
					throw new Exception("no roles found");

				while (result.next()) {
					roles.add(new Role(result.getInt("id"), result.getString("name")));
				}
			}

			tries = 0;
			try (PreparedStatement command2 = conn.prepareStatement(findRolePermissions)) {
				for (Role role : roles) {
					command2.setInt(1, role.getId());
					do {
						try {
							result2 = command2.executeQuery();
							catchTimeOut = false;
						} catch (SQLTimeoutException e) {
							catchTimeOut = true;
							if (tries++ > 3)
								throw e;
						}
					} while (catchTimeOut);

					List<Permission> rolePermissions = new ArrayList<>();

					if (!result2.next())
						throw new Exception("no role permission found");

					while (result2.next()) {
						rolePermissions.add(new Permission(result2.getInt(1), result2.getString(2)));
					}
					rolesWithPermissions
							.add(new RolePermissions(new Role(role.getId(), role.getName()), rolePermissions));
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return rolesWithPermissions;
	}

	//validation done
	@Override
	public RolePermissions find(int id) throws SQLException, Exception {
		RolePermissions roleWithPermissions = null;
		Role role = null;
		boolean catchTimeOut = false;
		int tries = 0;
		ResultSet result = null, result1 = null;
		String sqlRole = "Select * From roles where id=?";
		List<Permission> rolePermissionsList = new ArrayList<>();
		String sqlRolePermissions = "select P.* "
				+ "from permissions P JOIN rolepermissions RP ON P.id=RP.permission_id " + "where RP.role_id=?";
		try (Connection conn = db.getConnection()) {
			try (PreparedStatement statement = conn.prepareStatement(sqlRole)) {
				statement.setInt(1, id);

				do {
					try {
						result = statement.executeQuery();
						catchTimeOut = false;
					} catch (SQLTimeoutException e) {
						catchTimeOut = true;
						if (tries++ > 3)
							throw e;
					}
				} while (catchTimeOut);

				if (result.next()) {
					role = new Role(result.getInt(1), result.getString(2), result.getString(3));
				} else {
					throw new Exception("ID not found");
				}
			}

			tries = 0;
			try (PreparedStatement statement1 = conn.prepareStatement(sqlRolePermissions)) {
				statement1.setInt(1, role.getId());

				do {
					try {
						result1 = statement1.executeQuery();
						catchTimeOut = false;
					} catch (SQLTimeoutException e) {
						catchTimeOut = true;
						if (tries++ > 3)
							throw e;
					}
				} while (catchTimeOut);

				if (!result1.next())
					throw new EmployeeException("no role permission found");

				while (result1.next()) {
					rolePermissionsList.add(new Permission(result1.getInt(1), result1.getString(2)));
				}
			}
		} catch(Exception e) {
			throw e;
		}
		roleWithPermissions = new RolePermissions(role, rolePermissionsList);
		return roleWithPermissions;
	}

	
	
	@Override
	public RolePermissions add(RolePermissions roleWithPermissions) throws Exception {
		RolePermissions newRole = null;
		int roleId;
		boolean catchTimeOut = false;
		int tries = 0;
		ResultSet exists = null;
		List<Permission> rolePermissions = roleWithPermissions.getPermissions();
		String checkIfRoleExists = "select * from roles where name=?";
		String sqlAddRole = "Insert INTO roles (name,description) values(?,?)";
		String sqlLinkRoleWithpermission = "Insert INTO rolepermissions(role_id,permission_id) values(?,?)";

		try (Connection conn = db.getConnection()) {
			// check if the role already exists
			try (PreparedStatement state = conn.prepareStatement(checkIfRoleExists)) {
				state.setString(1, roleWithPermissions.getRole().getName());

				do {
					try {
						exists = state.executeQuery();
						catchTimeOut = false;
					} catch (SQLTimeoutException e) {
						catchTimeOut = true;
						if (tries++ > 3)
							throw e;
					}
				} while (catchTimeOut);

				tries = 0;

				// if the result set is false..there is no such role in the database
				if (!exists.next()) {
					try (PreparedStatement statement = conn.prepareStatement(sqlAddRole,
							Statement.RETURN_GENERATED_KEYS)) {
						statement.setString(1, roleWithPermissions.getRole().getName());
						statement.setString(2, roleWithPermissions.getRole().getDescription());

						do {
							try {
								statement.executeUpdate();
								catchTimeOut = false;
							} catch (SQLTimeoutException e) {
								catchTimeOut = true;
								if (tries++ > 3)
									throw e;
							}
						} while (catchTimeOut);

						ResultSet ids = null;
						try {
							ids = statement.getGeneratedKeys();
						} catch(SQLFeatureNotSupportedException e) {
							throw e;
						}
						if (!ids.next())
							throw new EmployeeException("no IDs found");

						while (ids.next()) {
							roleId = ids.getInt(1);
							newRole = find(roleId);
						}
					}

					tries = 0;

					try (PreparedStatement statement2 = conn.prepareStatement(sqlLinkRoleWithpermission)) {
						for (int i = 0; i < rolePermissions.size(); i++) {
							statement2.setInt(1, newRole.getRole().getId());
							statement2.setInt(2, rolePermissions.get(i).getId());
							do {
								try {
									int num = statement2.executeUpdate();
									if(num == 0)
										throw new Exception("update failed");
									catchTimeOut = false;
								} catch (SQLTimeoutException e) {
									catchTimeOut = true;
									if (tries++ > 3)
										throw e;
								}
							} while (catchTimeOut);
						}
					}
					try {
						newRole = find(newRole.getRole().getId());
					} catch(Exception e) {
						throw e;
					}
				} else {
					throw new EmployeeException("role already exists");
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return newRole;
	}

	@Override
	public RolePermissions update(RolePermissions role) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RolePermissions delete(int id) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

}
