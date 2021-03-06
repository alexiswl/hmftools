/*
 * This file is generated by jOOQ.
*/
package org.ensembl.database.homo_sapiens_core.tables.records;


import java.sql.Timestamp;

import javax.annotation.Generated;

import org.ensembl.database.homo_sapiens_core.tables.MappingSession;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record8;
import org.jooq.Row8;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.9.5"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class MappingSessionRecord extends UpdatableRecordImpl<MappingSessionRecord> implements Record8<UInteger, String, String, String, String, String, String, Timestamp> {

    private static final long serialVersionUID = -863023567;

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.mapping_session_id</code>.
     */
    public void setMappingSessionId(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.mapping_session_id</code>.
     */
    public UInteger getMappingSessionId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.old_db_name</code>.
     */
    public void setOldDbName(String value) {
        set(1, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.old_db_name</code>.
     */
    public String getOldDbName() {
        return (String) get(1);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.new_db_name</code>.
     */
    public void setNewDbName(String value) {
        set(2, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.new_db_name</code>.
     */
    public String getNewDbName() {
        return (String) get(2);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.old_release</code>.
     */
    public void setOldRelease(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.old_release</code>.
     */
    public String getOldRelease() {
        return (String) get(3);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.new_release</code>.
     */
    public void setNewRelease(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.new_release</code>.
     */
    public String getNewRelease() {
        return (String) get(4);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.old_assembly</code>.
     */
    public void setOldAssembly(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.old_assembly</code>.
     */
    public String getOldAssembly() {
        return (String) get(5);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.new_assembly</code>.
     */
    public void setNewAssembly(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.new_assembly</code>.
     */
    public String getNewAssembly() {
        return (String) get(6);
    }

    /**
     * Setter for <code>homo_sapiens_core_89_37.mapping_session.created</code>.
     */
    public void setCreated(Timestamp value) {
        set(7, value);
    }

    /**
     * Getter for <code>homo_sapiens_core_89_37.mapping_session.created</code>.
     */
    public Timestamp getCreated() {
        return (Timestamp) get(7);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record8 type implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public Row8<UInteger, String, String, String, String, String, String, Timestamp> fieldsRow() {
        return (Row8) super.fieldsRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Row8<UInteger, String, String, String, String, String, String, Timestamp> valuesRow() {
        return (Row8) super.valuesRow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<UInteger> field1() {
        return MappingSession.MAPPING_SESSION.MAPPING_SESSION_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field2() {
        return MappingSession.MAPPING_SESSION.OLD_DB_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field3() {
        return MappingSession.MAPPING_SESSION.NEW_DB_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field4() {
        return MappingSession.MAPPING_SESSION.OLD_RELEASE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field5() {
        return MappingSession.MAPPING_SESSION.NEW_RELEASE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field6() {
        return MappingSession.MAPPING_SESSION.OLD_ASSEMBLY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<String> field7() {
        return MappingSession.MAPPING_SESSION.NEW_ASSEMBLY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Field<Timestamp> field8() {
        return MappingSession.MAPPING_SESSION.CREATED;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UInteger value1() {
        return getMappingSessionId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value2() {
        return getOldDbName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value3() {
        return getNewDbName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value4() {
        return getOldRelease();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value5() {
        return getNewRelease();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value6() {
        return getOldAssembly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String value7() {
        return getNewAssembly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Timestamp value8() {
        return getCreated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value1(UInteger value) {
        setMappingSessionId(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value2(String value) {
        setOldDbName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value3(String value) {
        setNewDbName(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value4(String value) {
        setOldRelease(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value5(String value) {
        setNewRelease(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value6(String value) {
        setOldAssembly(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value7(String value) {
        setNewAssembly(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord value8(Timestamp value) {
        setCreated(value);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MappingSessionRecord values(UInteger value1, String value2, String value3, String value4, String value5, String value6, String value7, Timestamp value8) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached MappingSessionRecord
     */
    public MappingSessionRecord() {
        super(MappingSession.MAPPING_SESSION);
    }

    /**
     * Create a detached, initialised MappingSessionRecord
     */
    public MappingSessionRecord(UInteger mappingSessionId, String oldDbName, String newDbName, String oldRelease, String newRelease, String oldAssembly, String newAssembly, Timestamp created) {
        super(MappingSession.MAPPING_SESSION);

        set(0, mappingSessionId);
        set(1, oldDbName);
        set(2, newDbName);
        set(3, oldRelease);
        set(4, newRelease);
        set(5, oldAssembly);
        set(6, newAssembly);
        set(7, created);
    }
}
