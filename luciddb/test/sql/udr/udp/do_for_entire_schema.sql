-- test the do_for_entire_schema UDP

-- Perform first test in a schema with only one table; exec order is non-deterministic
create schema TESTSCHEMA_SIMPLE;
set schema 'TESTSCHEMA_SIMPLE';
create table T1(col1 integer, col2 integer);

-- try to do a select; should fail
call applib.do_for_entire_schema('select * from %TABLE_NAME%', 'TESTSCHEMA_SIMPLE', 'TABLES_AND_VIEWS');

create schema TESTSCHEMA;
set schema 'TESTSCHEMA';
create user MONDRIAN_USER authorization 'NOPE' default schema TESTSCHEMA;
create table T1(col1 integer, col2 integer);
create table T2(col3 varchar(255), col4 integer);
create view V1 as select * from T2;
create view V2 as select col2 from T1;

-- connect as new user and try to do select on tables. should fail.
!connect jdbc:luciddb: MONDRIAN_USER NOPE
select * from T1;
select * from V2;
!closeall

-- connect as sysadmin and give new user select privs for everything in schema
!connect jdbc:luciddb: sa sa
call applib.do_for_entire_schema('grant select on %TABLE_NAME% to MONDRIAN_USER', 'TESTSCHEMA', 'TABLES');

-- try again. should succeed for table but fail for view
!connect jdbc:luciddb: MONDRIAN_USER NOPE
select * from T1;
select * from V1;
!closeall
!connect jdbc:luciddb: sa sa
call applib.do_for_entire_schema('grant select on %TABLE_NAME% to MONDRIAN_USER', 'TESTSCHEMA', 'VIEWS');

-- should succeed for both this time
!connect jdbc:luciddb: MONDRIAN_USER NOPE
select * from T1;
select * from V1;
!closeall
!connect jdbc:luciddb: sa sa
drop table TESTSCHEMA.T1 cascade;
drop table TESTSCHEMA.T2 cascade;
drop user MONDRIAN_USER;
drop schema TESTSCHEMA;

-- if schema doesn't exist, it should give a nice error (LER-2608)
call applib.do_for_entire_schema('select * from %TABLE_NAME%', 'IMAGINARY_SCHEMA', 'TABLES');
