--
-- PostgreSQL database cluster dump
--

SET default_transaction_read_only = off;

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

--
-- Roles
--

CREATE ROLE bankx_user;
ALTER ROLE bankx_user WITH NOSUPERUSER INHERIT NOCREATEROLE NOCREATEDB LOGIN NOREPLICATION NOBYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:pc18ja5xgDsAsN7KSCRptw==$9b/SCcXVzwPqvOFDc0xRISswFE4CI2NdCQqR89p8DwA=:/eUPhV1yrMTKxkEJuVyDjLdqU0wDgWtK0a6DZu0/Eus=';
CREATE ROLE postgres;
ALTER ROLE postgres WITH SUPERUSER INHERIT CREATEROLE CREATEDB LOGIN REPLICATION BYPASSRLS PASSWORD 'SCRAM-SHA-256$4096:D8L3z1k7JTqQHQrTaYfIBw==$qmfYjtixVQuL/PtuWScOLxEl6FP9U1wpna1eBz5DTg8=:7F6aRslGA0FppzZMDPOP4v5fLIzJPOk08CDdeJh7j2Q=';

--
-- User Configurations
--








--
-- Databases
--

--
-- Database "template1" dump
--

\connect template1

--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13
-- Dumped by pg_dump version 15.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- PostgreSQL database dump complete
--

--
-- Database "bankx" dump
--

--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13
-- Dumped by pg_dump version 15.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: bankx; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE bankx WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.utf8';


ALTER DATABASE bankx OWNER TO postgres;

\connect bankx

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: accounts; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.accounts (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    customer_id uuid,
    account_number character varying(20) NOT NULL,
    account_type character varying(20) NOT NULL,
    balance numeric(15,2) DEFAULT 0.00,
    status character varying(20) DEFAULT 'ACTIVE'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT accounts_account_type_check CHECK (((account_type)::text = ANY ((ARRAY['CURRENT'::character varying, 'SAVINGS'::character varying])::text[])))
);


ALTER TABLE public.accounts OWNER TO postgres;

--
-- Name: customers; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.customers (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    email character varying(255) NOT NULL,
    phone character varying(20),
    id_number character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.customers OWNER TO postgres;

--
-- Name: transactions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.transactions (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    account_id uuid,
    transaction_type character varying(20) NOT NULL,
    amount numeric(15,2) NOT NULL,
    balance_after numeric(15,2) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'COMPLETED'::character varying,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.transactions OWNER TO postgres;

--
-- Data for Name: accounts; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.accounts (id, customer_id, account_number, account_type, balance, status, created_at) FROM stdin;
a3c6a557-7a38-4fa3-8b44-e4c3bbace757	14c68064-99ec-4b90-b215-d79944b95d4e	4576809286	SAVINGS	33925.00	ACTIVE	2025-07-08 02:38:19.808928
679d3d10-3712-43b5-98bd-3b7bcd6b7aac	14c68064-99ec-4b90-b215-d79944b95d4e	4311626311	CURRENT	17399.00	ACTIVE	2025-07-08 02:38:19.81071
bd09062f-866f-43ad-874e-3bcd0bf296bb	c6571051-dda4-41c6-87d1-e0b479e94b18	4790702395	SAVINGS	46730.00	ACTIVE	2025-07-08 02:38:19.815771
3bc07f78-e190-4079-9717-6a56e0d97500	c6571051-dda4-41c6-87d1-e0b479e94b18	4257494189	CURRENT	5242.00	ACTIVE	2025-07-08 02:38:19.816663
2aa2b4af-523b-4b3f-af95-34438a2a8d24	e9ffe4cf-8981-4ee2-bc1a-e7643aedd0cb	4537119956	SAVINGS	50303.00	ACTIVE	2025-07-08 02:38:19.820992
7c91e524-8b0e-4a35-b99c-7d9fd152512b	e9ffe4cf-8981-4ee2-bc1a-e7643aedd0cb	4299823459	CURRENT	15261.00	ACTIVE	2025-07-08 02:38:19.821801
\.


--
-- Data for Name: customers; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.customers (id, first_name, last_name, email, phone, id_number, created_at) FROM stdin;
14c68064-99ec-4b90-b215-d79944b95d4e	John	Doe	john.doe@email.com	+27 11 123 4567	8501015800083	2025-07-08 02:38:19.807623
c6571051-dda4-41c6-87d1-e0b479e94b18	Jane	Smith	jane.smith@email.com	+27 21 987 6543	9203127890123	2025-07-08 02:38:19.814926
e9ffe4cf-8981-4ee2-bc1a-e7643aedd0cb	Mike	Johnson	mike.johnson@email.com	+27 31 555 0123	7809081234567	2025-07-08 02:38:19.820327
\.


--
-- Data for Name: transactions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.transactions (id, account_id, transaction_type, amount, balance_after, description, status, created_at) FROM stdin;
510cd7b5-e36c-4d67-a262-c3e2824aa2b3	a3c6a557-7a38-4fa3-8b44-e4c3bbace757	WITHDRAWAL	533.00	9034.00	Sample withdrawal	COMPLETED	2025-07-08 02:38:19.811535
bc1158f2-76d7-42e7-a0c2-4abc58446f5d	679d3d10-3712-43b5-98bd-3b7bcd6b7aac	DEPOSIT	306.00	1464.00	Sample deposit	COMPLETED	2025-07-08 02:38:19.812894
2cddea52-20d7-40d3-a420-7ca008c20e8a	a3c6a557-7a38-4fa3-8b44-e4c3bbace757	WITHDRAWAL	169.00	10423.00	Sample withdrawal	COMPLETED	2025-07-08 02:38:19.813856
e46929b7-9fd1-4dd7-a8eb-42eb0efcb986	bd09062f-866f-43ad-874e-3bcd0bf296bb	WITHDRAWAL	694.00	10499.00	Sample withdrawal	COMPLETED	2025-07-08 02:38:19.817531
ace86f3b-b17b-4678-b9bc-1175f9c2ba17	bd09062f-866f-43ad-874e-3bcd0bf296bb	DEPOSIT	121.00	5570.00	Sample deposit	COMPLETED	2025-07-08 02:38:19.818379
fc8a4c9a-4dfd-4701-b674-d70b8105124d	bd09062f-866f-43ad-874e-3bcd0bf296bb	DEPOSIT	267.00	7995.00	Sample deposit	COMPLETED	2025-07-08 02:38:19.819425
ce0e3e38-6042-4c40-af37-fffc2468c1e4	7c91e524-8b0e-4a35-b99c-7d9fd152512b	WITHDRAWAL	614.00	6596.00	Sample withdrawal	COMPLETED	2025-07-08 02:38:19.822501
9c84e8b4-bb3f-4a0b-b959-a8f13782e592	7c91e524-8b0e-4a35-b99c-7d9fd152512b	DEPOSIT	902.00	1262.00	Sample deposit	COMPLETED	2025-07-08 02:38:19.823229
add441a2-5faa-4447-a256-f98111bda0f8	2aa2b4af-523b-4b3f-af95-34438a2a8d24	WITHDRAWAL	139.00	8089.00	Sample withdrawal	COMPLETED	2025-07-08 02:38:19.824048
\.


--
-- Name: accounts accounts_account_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_account_number_key UNIQUE (account_number);


--
-- Name: accounts accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_pkey PRIMARY KEY (id);


--
-- Name: customers customers_email_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_email_key UNIQUE (email);


--
-- Name: customers customers_id_number_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_id_number_key UNIQUE (id_number);


--
-- Name: customers customers_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.customers
    ADD CONSTRAINT customers_pkey PRIMARY KEY (id);


--
-- Name: transactions transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_pkey PRIMARY KEY (id);


--
-- Name: accounts accounts_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.accounts
    ADD CONSTRAINT accounts_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.customers(id);


--
-- Name: transactions transactions_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.transactions
    ADD CONSTRAINT transactions_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.accounts(id);


--
-- PostgreSQL database dump complete
--

--
-- Database "bankx_db" dump
--

--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13
-- Dumped by pg_dump version 15.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: bankx_db; Type: DATABASE; Schema: -; Owner: postgres
--

CREATE DATABASE bankx_db WITH TEMPLATE = template0 ENCODING = 'UTF8' LOCALE_PROVIDER = libc LOCALE = 'en_US.utf8';


ALTER DATABASE bankx_db OWNER TO postgres;

\connect bankx_db

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: DATABASE bankx_db; Type: ACL; Schema: -; Owner: postgres
--

GRANT ALL ON DATABASE bankx_db TO bankx_user;


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: pg_database_owner
--

GRANT ALL ON SCHEMA public TO bankx_user;


--
-- PostgreSQL database dump complete
--

--
-- Database "postgres" dump
--

\connect postgres

--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13
-- Dumped by pg_dump version 15.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- PostgreSQL database dump complete
--

--
-- PostgreSQL database cluster dump complete
--

