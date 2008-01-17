package com.finalist.cmsc.services.community.dao;

import java.util.Map;

import com.finalist.cmsc.services.community.data.User;

/**
 * UserDAO, this is a hibernate DAO/transaction interface.
 * This interface is implemented by the implementation classes.
 * it contains the methods for transactions to the database
 * 
 * @author menno menninga
 */
public interface UserDAO extends DAO<User>{

   public Map<String, Map<String, String>> getUserProperty(String userName);
}
