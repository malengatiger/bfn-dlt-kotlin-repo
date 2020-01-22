CREATE USER "user1" WITH LOGIN PASSWORD 'test';
CREATE SCHEMA "party_a_schema";
GRANT USAGE, CREATE ON SCHEMA "party_a_schema" TO "user1";
GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON ALL tables IN SCHEMA "party_a_schema" TO "user1";
ALTER DEFAULT privileges IN SCHEMA "party_a_schema" GRANT SELECT, INSERT, UPDATE, DELETE, REFERENCES ON tables TO "user1";
GRANT USAGE, SELECT ON ALL sequences IN SCHEMA "party_a_schema" TO "user1";
ALTER DEFAULT privileges IN SCHEMA "party_a_schema" GRANT USAGE, SELECT ON sequences TO "user1";
ALTER ROLE "user1" SET search_path = "party_a_schema";
