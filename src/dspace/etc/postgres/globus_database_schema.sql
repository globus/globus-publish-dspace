--
-- globus_database_schema.sql
--
-- Version: $Revision$
--
-- Date:    $Date$
--
ALTER TABLE TasklistItem
ADD eperson_group_id INTEGER REFERENCES EPersonGroup(eperson_group_id);

CREATE INDEX tasklist_group_idx on TasklistItem(eperson_group_id);

CREATE SEQUENCE globusconfig_seq;

CREATE TABLE GlobusConfig
(
    globus_config_id INTEGER PRIMARY KEY DEFAULT NEXTVAL('globusconfig_seq'),
    dso_id INTEGER,
    dso_type INTEGER,
    config_name VARCHAR(64),
    config_group VARCHAR(64),
    config_value TEXT
);

ALTER TABLE GlobusConfig ADD COLUMN config_group VARCHAR(64);

-- Lookups via the id and dso_type and property name
CREATE INDEX globusconfig_dsoidconfig_idx on GlobusConfig(dso_id,dso_type,config_name);

-- Remove unique email constraint as Globus auth allows the same email address to be used for different identities
ALTER TABLE eperson DROP CONSTRAINT eperson_email_key;
