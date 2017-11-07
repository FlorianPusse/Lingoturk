package models;

import be.objectify.deadbolt.java.models.Permission;
import be.objectify.deadbolt.java.models.Role;
import be.objectify.deadbolt.java.models.Subject;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.NotNull;
import org.mindrot.jbcrypt.BCrypt;

import javax.persistence.*;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents the users of this LingoTurk installation
 */
@Entity
@Table(name = "LingoturkUsers")
public class User extends Model implements Subject {

    /**
     * The name of the user
     */
    @Id
    @Basic
    private String username;

    /**
     * The hashed password of the user
     */
    @Basic
    @NotNull
    private String hash;

    /**
     * The current session token for this user
     */
    @Basic
    @NotNull
    private String token;

    /**
     * The time it takes until a token expires after the user logs in (default 24 hours)
     */
    private static final long EXPIRY_TIME = 1000 * 60 * 60 * 24;

    /**
     * The expiry date for the current session token
     */
    @NotNull
    @Column(name = "expirydate", columnDefinition = "timestamp default now()")
    private Date expiryDate;

    /**
     * The time this user logged in the last time
     */
    @NotNull
    @Column(name = "lastlogin", columnDefinition = "timestamp default now()")
    private Date lastlogin;

    private static final Finder<String, User> find = new Finder<>(User.class);

    public User(String username, String plaintextPassword) {
        this.username = username;
        this.hash = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());
        this.token = "";
        lastlogin = new Date();
        expiryDate = new Date(lastlogin.getTime());
    }

    /**
     * The roles a user has, e.g. admin, experimenter, ...
     * TODO: Implement useful roles
     *
     * @return The list of roles this user has
     */
    @Override
    public List<? extends Role> getRoles() {
        return new LinkedList<>();
    }

    /**
     * The permissions a user has, e.g. "can create experiments". Most of this will be handeled by roles
     *
     * @return The list of permissions this user has
     */
    @Override
    public List<? extends Permission> getPermissions() {
        return new LinkedList<>();
    }

    /**
     * Returns the name identifier (i.e. the name) of this User
     *
     * @return The name of this user
     */
    @Override
    public String getIdentifier() {
        return username;
    }

    /**
     * Log in a user. If the password is correct, a new session token is returned. Otherwise
     * null is returned
     *
     * @param password The password that the user provided
     * @return A new session token, if the password is correct. Null otherwise
     */
    public String login(String password) {
        if (!BCrypt.checkpw(password, hash)) {
            return null;
        }

        lastlogin = new Date();
        expiryDate = new Date(lastlogin.getTime() + EXPIRY_TIME);
        String unHashedToken = hash + expiryDate.getTime();
        token = BCrypt.hashpw(unHashedToken, BCrypt.gensalt());
        update();

        return token;
    }

    /**
     * Verifies a given token against the stored one and checks if it has not expired yet
     *
     * @param token The token the user provided
     * @return Returns true iff the token is correct and has not expired yet
     */
    public boolean verifyToken(String token) {
        return this.token.equals(token) && expiryDate.after(new Date());
    }

    /**
     * Updates this users password, if the old password is correct
     *
     * @param oldpassword The old password the user provides
     * @param plaintext   The new password (in plaintext)
     * @return Returns true iff the update process succeeded.
     */
    public boolean updatePassword(String oldpassword, String plaintext) {
        if (oldpassword.isEmpty() || plaintext.isEmpty()) {
            return false;
        }

        if (!BCrypt.checkpw(oldpassword, hash)) {
            return false;
        }

        hash = BCrypt.hashpw(plaintext, BCrypt.gensalt());
        update();

        return true;
    }

    /**
     * Checks and returns if any user is currently present in the database.
     *
     * @return Returns true iff at least one user exists in the database.
     */
    public static boolean existsAny() {
        return find.nativeSql("SELECT 1 FROM LingoturkUsers").findCount() > 0;
    }

    /**
     * Retrieves an user form the database given its specifier {@code username}. If no such
     * experimenter exists, null is be returned
     *
     * @param username The name of the user
     * @return The user with specifier {@code username}, or null if no such user exists
     */
    public static User byUsername(String username) {
        return find.byId(username);
    }

}