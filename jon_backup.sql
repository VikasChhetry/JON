-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: jon
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `approval_history`
--

DROP TABLE IF EXISTS `approval_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) NOT NULL,
  `comments` text,
  `new_status` varchar(255) DEFAULT NULL,
  `previous_status` varchar(255) DEFAULT NULL,
  `rejection_reason` text,
  `timestamp` datetime(6) NOT NULL,
  `user_role` enum('ADMIN','FACULTY','HOD','STUDENT') NOT NULL,
  `performed_by` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  `new_approved_team_size` int DEFAULT NULL,
  `new_requested_team_size` int DEFAULT NULL,
  `previous_approved_team_size` int DEFAULT NULL,
  `previous_requested_team_size` int DEFAULT NULL,
  `new_max_team_size` int DEFAULT NULL,
  `new_min_team_size` int DEFAULT NULL,
  `previous_max_team_size` int DEFAULT NULL,
  `previous_min_team_size` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqvpwaecik1ywouwk9dq525eng` (`performed_by`),
  KEY `FKg93lfa2ayw69b36a3gtj5vb7t` (`project_id`),
  CONSTRAINT `FKg93lfa2ayw69b36a3gtj5vb7t` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`),
  CONSTRAINT `FKqvpwaecik1ywouwk9dq525eng` FOREIGN KEY (`performed_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=102 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `approval_history`
--

LOCK TABLES `approval_history` WRITE;
/*!40000 ALTER TABLE `approval_history` DISABLE KEYS */;
INSERT INTO `approval_history` VALUES (25,'Selected project topic',NULL,'TOPIC_SELECTED',NULL,NULL,'2026-09-09 04:04:15.587181','STUDENT',6,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(26,'Started working on project',NULL,'STUDENT_WORKING','TOPIC_SELECTED',NULL,'2026-09-09 04:04:18.950221','STUDENT',6,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(27,'Submitted project for faculty review',NULL,'PENDING_FACULTY_REVIEW','STUDENT_WORKING',NULL,'2026-09-09 04:04:33.339651','STUDENT',6,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(28,'Faculty approved project','looks good ','FACULTY_APPROVED','PENDING_FACULTY_REVIEW',NULL,'2026-09-09 04:05:27.406825','FACULTY',10,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(29,'Sent to HOD for review',NULL,'PENDING_HOD_REVIEW','FACULTY_APPROVED',NULL,'2026-09-09 04:05:27.409825','FACULTY',10,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(30,'HOD approved project — COMPLETED','looks good ','COMPLETED','PENDING_HOD_REVIEW',NULL,'2026-09-09 04:12:33.866161','HOD',7,9,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(68,'Selected project topic',NULL,'TOPIC_SELECTED',NULL,NULL,'2026-09-11 19:08:26.360359','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(69,'Submitted project proposal for faculty review',NULL,'PROPOSAL_PENDING_FACULTY','TOPIC_SELECTED',NULL,'2026-09-11 19:23:28.096154','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(70,'Rejected project proposal','','FACULTY_REJECTED','PROPOSAL_PENDING_FACULTY','tema member is missing','2026-09-11 19:24:48.701158','FACULTY',5,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(71,'Student resubmitted project','','STUDENT_RESUBMISSION','FACULTY_REJECTED',NULL,'2026-09-15 01:08:48.755231','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(72,'Routed back for review',NULL,'PROPOSAL_PENDING_FACULTY','STUDENT_RESUBMISSION',NULL,'2026-09-15 01:08:48.797119','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(73,'Approved project proposal','','PROPOSAL_APPROVED','PROPOSAL_PENDING_FACULTY',NULL,'2026-09-15 01:21:09.216640','FACULTY',5,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(74,'Started working on project',NULL,'STUDENT_WORKING','PROPOSAL_APPROVED',NULL,'2026-09-15 01:21:31.964730','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(75,'Submitted final project for faculty review',NULL,'PENDING_FACULTY_REVIEW','STUDENT_WORKING',NULL,'2026-09-15 01:22:32.250355','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(76,'Faculty approved project','','FACULTY_APPROVED','PENDING_FACULTY_REVIEW',NULL,'2026-09-15 01:23:00.053737','FACULTY',5,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(77,'Sent to HOD for review',NULL,'PENDING_HOD_REVIEW','FACULTY_APPROVED',NULL,'2026-09-15 01:23:00.057490','FACULTY',5,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(78,'HOD rejected project','','HOD_REJECTED','PENDING_HOD_REVIEW','sxsasa','2026-09-15 01:28:32.328910','HOD',7,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(79,'Student resubmitted project','asdasasa','STUDENT_RESUBMISSION','HOD_REJECTED',NULL,'2026-09-15 01:28:52.254207','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(80,'Routed back for review',NULL,'PENDING_HOD_REVIEW','STUDENT_RESUBMISSION',NULL,'2026-09-15 01:28:52.256213','STUDENT',6,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(81,'HOD approved project — COMPLETED','looks good ','COMPLETED','PENDING_HOD_REVIEW',NULL,'2026-09-15 01:31:02.620899','HOD',7,17,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `approval_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch`
--

DROP TABLE IF EXISTS `branch`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch` (
  `id` bigint NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch`
--

LOCK TABLES `branch` WRITE;
/*!40000 ALTER TABLE `branch` DISABLE KEYS */;
INSERT INTO `branch` VALUES (1,'CSE'),(2,'IT'),(3,'ECE');
/*!40000 ALTER TABLE `branch` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branch_seq`
--

DROP TABLE IF EXISTS `branch_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branch_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branch_seq`
--

LOCK TABLES `branch_seq` WRITE;
/*!40000 ALTER TABLE `branch_seq` DISABLE KEYS */;
INSERT INTO `branch_seq` VALUES (1);
/*!40000 ALTER TABLE `branch_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `branches`
--

DROP TABLE IF EXISTS `branches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `branches` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `code` varchar(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `name` varchar(100) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKrt29b5cpquhexus5t5ywalg67` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `branches`
--

LOCK TABLES `branches` WRITE;
/*!40000 ALTER TABLE `branches` DISABLE KEYS */;
INSERT INTO `branches` VALUES (1,'CSE001','2026-09-07 03:24:19.498404','CSE'),(2,'CS002','2026-09-07 03:31:21.371005','CS'),(4,'IT003','2026-09-09 01:30:52.775407','IT');
/*!40000 ALTER TABLE `branches` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faculty_topic`
--

DROP TABLE IF EXISTS `faculty_topic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faculty_topic` (
  `id` bigint NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `guide_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnfwfr73salhq64yeqy9bmtwr1` (`guide_id`),
  CONSTRAINT `FKnfwfr73salhq64yeqy9bmtwr1` FOREIGN KEY (`guide_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faculty_topic`
--

LOCK TABLES `faculty_topic` WRITE;
/*!40000 ALTER TABLE `faculty_topic` DISABLE KEYS */;
INSERT INTO `faculty_topic` VALUES (1,'AI based attendance management system','SELECTED','AI Based Attendance System Updatedwww',302),(52,'AI based attendance tracking system','AVAILABLE','Smart Attendance Management System',302);
/*!40000 ALTER TABLE `faculty_topic` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `faculty_topic_seq`
--

DROP TABLE IF EXISTS `faculty_topic_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `faculty_topic_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `faculty_topic_seq`
--

LOCK TABLES `faculty_topic_seq` WRITE;
/*!40000 ALTER TABLE `faculty_topic_seq` DISABLE KEYS */;
INSERT INTO `faculty_topic_seq` VALUES (151);
/*!40000 ALTER TABLE `faculty_topic_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guide_assignment_history`
--

DROP TABLE IF EXISTS `guide_assignment_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guide_assignment_history` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `action` varchar(255) NOT NULL,
  `performer_role` enum('ADMIN','FACULTY','HOD','STUDENT') NOT NULL,
  `reason` text,
  `timestamp` datetime(6) NOT NULL,
  `branch_id` bigint NOT NULL,
  `new_faculty_id` bigint DEFAULT NULL,
  `performed_by` bigint NOT NULL,
  `previous_faculty_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK8llg62ievjjco4nfuiiq6sw86` (`branch_id`),
  KEY `FKcjkc9af3l8ddxxthy60916101` (`new_faculty_id`),
  KEY `FKj916noeuusb8pqc6csa1kp1cg` (`performed_by`),
  KEY `FKaib9rqmj5ac9fy6a5m8e3d5bl` (`previous_faculty_id`),
  KEY `FKjbucwvlxlm68oiby65to62w5g` (`student_id`),
  CONSTRAINT `FK8llg62ievjjco4nfuiiq6sw86` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`),
  CONSTRAINT `FKaib9rqmj5ac9fy6a5m8e3d5bl` FOREIGN KEY (`previous_faculty_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKcjkc9af3l8ddxxthy60916101` FOREIGN KEY (`new_faculty_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKj916noeuusb8pqc6csa1kp1cg` FOREIGN KEY (`performed_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKjbucwvlxlm68oiby65to62w5g` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=51 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guide_assignment_history`
--

LOCK TABLES `guide_assignment_history` WRITE;
/*!40000 ALTER TABLE `guide_assignment_history` DISABLE KEYS */;
INSERT INTO `guide_assignment_history` VALUES (1,'Student selected faculty guide','STUDENT','Self-selected','2026-09-07 04:44:52.523472',2,5,6,NULL,6),(2,'Faculty removed student','FACULTY','yes','2026-09-07 04:45:52.344871',2,NULL,5,5,6),(3,'Student selected faculty guide','STUDENT','Self-selected','2026-09-07 04:46:37.566306',2,5,6,NULL,6),(4,'HOD removed student from faculty','HOD','HOD removed assignment','2026-09-07 04:48:03.441850',2,NULL,7,5,6),(5,'Student selected faculty guide','STUDENT','Self-selected','2026-09-07 04:48:53.323242',2,5,6,NULL,6),(6,'Auto-assigned student to faculty','HOD','Automatic assignment by system','2026-09-07 22:07:01.120489',2,8,7,NULL,11),(7,'HOD removed student from faculty','HOD','HOD removed assignment','2026-09-07 22:08:21.609696',2,NULL,7,8,11),(8,'HOD assigned student to faculty','HOD','ok','2026-09-07 22:08:30.750012',2,8,7,NULL,11),(9,'Faculty removed student','FACULTY','test ','2026-09-07 22:09:49.255459',2,NULL,8,8,11),(10,'Student selected faculty guide','STUDENT','Self-selected','2026-09-07 22:13:01.969460',2,10,11,NULL,11),(11,'Student removed self-selected guide','STUDENT','Student removed own selection','2026-09-07 22:22:50.367916',2,NULL,6,5,6),(12,'Student selected faculty guide','STUDENT','Self-selected','2026-09-07 22:23:19.671531',2,5,6,NULL,6),(13,'HOD REASSIGNED GUIDE','HOD','HOD reassignment','2026-09-09 01:43:57.037150',2,8,7,5,6),(14,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-09 01:45:10.035160',2,NULL,7,8,6),(15,'HOD REASSIGNED GUIDE','HOD','HOD reassignment','2026-09-09 01:45:53.989078',2,5,7,10,11),(16,'HOD ASSIGNED GUIDE','HOD','a','2026-09-09 01:46:02.162853',2,10,7,NULL,6),(17,'Auto-assigned student to faculty','HOD','Automatic assignment by system','2026-09-11 14:30:13.671267',1,3,4,NULL,2),(18,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-11 15:06:37.612500',2,8,7,NULL,13),(19,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 15:08:39.738307',2,NULL,7,10,6),(20,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 15:08:43.655360',2,NULL,7,5,11),(21,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 15:08:47.009905',2,NULL,7,8,13),(22,'Student selected faculty guide','STUDENT','Self-selected','2026-09-11 15:09:42.831864',2,5,6,NULL,6),(23,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-11 15:28:35.303089',2,8,7,NULL,11),(24,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-11 15:28:35.317589',2,9,7,NULL,13),(25,'HOD REASSIGNED GUIDE','HOD','HOD reassignment','2026-09-11 15:28:43.726006',2,10,7,8,11),(26,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 15:30:30.584478',2,NULL,7,9,13),(27,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 15:30:33.765883',2,NULL,7,5,6),(28,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 15:30:36.613667',2,NULL,7,10,11),(29,'Student selected faculty guide','STUDENT','Self-selected','2026-09-11 17:28:35.680132',2,5,6,NULL,6),(30,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-11 17:36:18.080917',2,8,7,NULL,11),(31,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-11 17:36:18.096254',2,9,7,NULL,13),(32,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 17:37:23.132421',2,NULL,7,5,6),(33,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 17:37:26.559073',2,NULL,7,8,11),(34,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-11 17:37:30.380436',2,NULL,7,9,13),(35,'Student selected faculty guide','STUDENT','Self-selected','2026-09-11 17:37:57.148134',2,5,6,NULL,6),(36,'Student removed self-selected guide','STUDENT','Student removed own selection','2026-09-11 17:38:01.480487',2,NULL,6,5,6),(37,'Student selected faculty guide','STUDENT','Self-selected','2026-09-11 17:38:05.101374',2,9,6,NULL,6),(38,'Student removed self-selected guide','STUDENT','Student removed own selection','2026-09-11 17:38:08.823446',2,NULL,6,9,6),(39,'Student selected faculty guide','STUDENT','Self-selected','2026-09-11 17:38:17.292291',2,5,6,NULL,6),(40,'Student selected faculty guide','STUDENT','Self-selected','2026-09-11 17:38:35.066783',2,10,13,NULL,13),(41,'HOD ASSIGNED GUIDE','HOD','ok','2026-09-15 01:36:18.118219',2,5,7,NULL,11),(42,'HOD ASSIGNED GUIDE','HOD','a','2026-09-15 15:53:33.334501',2,5,7,NULL,15),(43,'HOD ASSIGNED GUIDE','HOD','ok','2026-09-15 15:53:41.974519',2,8,7,NULL,16),(44,'HOD REMOVED GUIDE','HOD','HOD removed assignment','2026-09-15 16:52:21.059807',2,NULL,7,5,11),(45,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-23 05:41:39.065894',2,9,7,NULL,11),(46,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-23 05:41:39.082707',2,8,7,NULL,17),(47,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-23 05:41:39.095488',2,9,7,NULL,18),(48,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-23 05:41:39.106402',2,5,7,NULL,19),(49,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-23 05:41:39.116979',2,8,7,NULL,20),(50,'AUTO ASSIGNED GUIDE','HOD','Automatically assigned after guide selection period ended','2026-09-23 05:41:39.129711',2,8,7,NULL,21);
/*!40000 ALTER TABLE `guide_assignment_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guide_assignments`
--

DROP TABLE IF EXISTS `guide_assignments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guide_assignments` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `assigned_at` datetime(6) NOT NULL,
  `assigned_by` varchar(20) NOT NULL,
  `branch_id` bigint NOT NULL,
  `faculty_id` bigint NOT NULL,
  `selection_form_id` bigint DEFAULT NULL,
  `student_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UKi04i3fqp91nhr7ffoj9xqjox1` (`student_id`),
  KEY `FK4nfcyj0jniupbts3j9c34nqja` (`branch_id`),
  KEY `FKj8t75w92nvmy81w3tqu3gswqy` (`faculty_id`),
  KEY `FK1tmvms3d0kr53r8l5e1ydk19d` (`selection_form_id`),
  CONSTRAINT `FK1tmvms3d0kr53r8l5e1ydk19d` FOREIGN KEY (`selection_form_id`) REFERENCES `guide_selection_forms` (`id`),
  CONSTRAINT `FK4nfcyj0jniupbts3j9c34nqja` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`),
  CONSTRAINT `FKj8t75w92nvmy81w3tqu3gswqy` FOREIGN KEY (`faculty_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKp2iw68ksxmi4h6ali54hxgugi` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guide_assignments`
--

LOCK TABLES `guide_assignments` WRITE;
/*!40000 ALTER TABLE `guide_assignments` DISABLE KEYS */;
INSERT INTO `guide_assignments` VALUES (14,'2026-09-11 14:30:13.656196','AUTO',1,3,2,2),(24,'2026-09-11 17:38:17.289782','STUDENT',2,5,1,6),(25,'2026-09-11 17:38:35.063005','STUDENT',2,10,1,13),(27,'2026-09-15 15:53:33.259150','HOD',2,5,1,15),(28,'2026-09-15 15:53:41.968758','HOD',2,8,1,16),(29,'2026-09-23 05:41:39.015184','AUTO',2,9,1,11),(30,'2026-09-23 05:41:39.081286','AUTO',2,8,1,17),(31,'2026-09-23 05:41:39.092908','AUTO',2,9,1,18),(32,'2026-09-23 05:41:39.103772','AUTO',2,5,1,19),(33,'2026-09-23 05:41:39.115350','AUTO',2,8,1,20),(34,'2026-09-23 05:41:39.127714','AUTO',2,8,1,21);
/*!40000 ALTER TABLE `guide_assignments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `guide_selection_forms`
--

DROP TABLE IF EXISTS `guide_selection_forms`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `guide_selection_forms` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `active` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `end_date_time` datetime(6) NOT NULL,
  `start_date_time` datetime(6) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `branch_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `auto_processed` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK2s7uk163a45tt02okssyouqdm` (`branch_id`),
  KEY `FKnp21hr4aa7piblyk97vwl0ptn` (`created_by`),
  CONSTRAINT `FK2s7uk163a45tt02okssyouqdm` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`),
  CONSTRAINT `FKnp21hr4aa7piblyk97vwl0ptn` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `guide_selection_forms`
--

LOCK TABLES `guide_selection_forms` WRITE;
/*!40000 ALTER TABLE `guide_selection_forms` DISABLE KEYS */;
INSERT INTO `guide_selection_forms` VALUES (1,_binary '','2026-09-07 04:42:59.001518','2026-09-16 19:29:00.000000','2026-09-14 17:27:00.000000','2026-09-15 16:53:56.766880',2,7,_binary ''),(2,_binary '','2026-09-11 14:29:45.343121','2026-09-12 14:29:00.000000','2026-09-11 14:29:00.000000','2026-09-15 00:54:22.839590',1,4,_binary '');
/*!40000 ALTER TABLE `guide_selection_forms` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `password_reset_tokens`
--

DROP TABLE IF EXISTS `password_reset_tokens`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `password_reset_tokens` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `expiry_date` datetime(6) NOT NULL,
  `token` varchar(255) NOT NULL,
  `user_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK71lqwbwtklmljk3qlsugr1mig` (`token`),
  KEY `FKk3ndxg5xp6v7wd4gjyusp15gq` (`user_id`),
  CONSTRAINT `FKk3ndxg5xp6v7wd4gjyusp15gq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `password_reset_tokens`
--

LOCK TABLES `password_reset_tokens` WRITE;
/*!40000 ALTER TABLE `password_reset_tokens` DISABLE KEYS */;
INSERT INTO `password_reset_tokens` VALUES (8,'2026-09-09 03:37:03.679398','6f436900-c153-4fe8-a3e9-2be1ec0bb465',2);
/*!40000 ALTER TABLE `password_reset_tokens` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `id` bigint NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `project_type` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `student_id` bigint DEFAULT NULL,
  `rejection_reason` varchar(255) DEFAULT NULL,
  `faculty_topic_id` bigint DEFAULT NULL,
  `branch_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKi4tku3pl3mt4sn6p950hjlva` (`student_id`),
  KEY `FKg2gn2l67bmt8ojtp8ktkifvpd` (`faculty_topic_id`),
  KEY `FKiysrpckv8iw454brxy5tin6gg` (`branch_id`),
  CONSTRAINT `FKg2gn2l67bmt8ojtp8ktkifvpd` FOREIGN KEY (`faculty_topic_id`) REFERENCES `faculty_topic` (`id`),
  CONSTRAINT `FKi4tku3pl3mt4sn6p950hjlva` FOREIGN KEY (`student_id`) REFERENCES `user` (`id`),
  CONSTRAINT `FKiysrpckv8iw454brxy5tin6gg` FOREIGN KEY (`branch_id`) REFERENCES `branch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project`
--

LOCK TABLES `project` WRITE;
/*!40000 ALTER TABLE `project` DISABLE KEYS */;
INSERT INTO `project` VALUES (1,'Improved digital platform','OWN_IDEA','PENDING','Jon System',152,NULL,NULL,NULL),(2,'Digital project approval platform','OWN_IDEA','HOD_APPROVED','Academic Project Approval System',152,NULL,NULL,NULL),(52,'Develop an attendance management system using AI','FACULTY_TOPIC','PENDING','AI Based Attendance System',302,NULL,1,NULL),(53,'Develop an attendance management system using AI','FACULTY_TOPIC','PENDING','AI Based Attendance System',302,NULL,1,NULL),(102,'Develop an attendance management system using AI','FACULTY_TOPIC','PENDING','AI Based Attendance System',152,NULL,1,NULL),(152,'AI based attendance tracking system','FACULTY_TOPIC','DROPPED','Smart Attendance Management System',152,NULL,52,NULL);
/*!40000 ALTER TABLE `project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_seq`
--

DROP TABLE IF EXISTS `project_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_seq`
--

LOCK TABLES `project_seq` WRITE;
/*!40000 ALTER TABLE `project_seq` DISABLE KEYS */;
INSERT INTO `project_seq` VALUES (251);
/*!40000 ALTER TABLE `project_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_team_members`
--

DROP TABLE IF EXISTS `project_team_members`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_team_members` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `added_at` datetime(6) NOT NULL,
  `contribution_role` varchar(200) NOT NULL,
  `is_owner` bit(1) NOT NULL,
  `member_id` bigint NOT NULL,
  `project_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK2wfn3yt6n40tk2kaj3qe42wi5` (`project_id`,`member_id`),
  KEY `FKf1ela21huc6ulbednglnrd1tp` (`member_id`),
  CONSTRAINT `FKf1ela21huc6ulbednglnrd1tp` FOREIGN KEY (`member_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKrplt19ljycvlrk9fy72yep75e` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_team_members`
--

LOCK TABLES `project_team_members` WRITE;
/*!40000 ALTER TABLE `project_team_members` DISABLE KEYS */;
INSERT INTO `project_team_members` VALUES (1,'2026-09-11 19:08:28.662930','Team Leader / Project Owner',_binary '',6,17),(2,'2026-09-11 19:08:45.974166','database',_binary '\0',11,17),(3,'2026-09-11 19:09:20.702276','swsws',_binary '\0',13,17),(4,'2026-09-11 19:22:56.944311','sdew',_binary '\0',15,17);
/*!40000 ALTER TABLE `project_team_members` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `project_topics`
--

DROP TABLE IF EXISTS `project_topics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_topics` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `status` enum('ARCHIVED','ASSIGNED','AVAILABLE') NOT NULL,
  `tech_stack` varchar(300) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `branch_id` bigint NOT NULL,
  `created_by` bigint NOT NULL,
  `max_team_size` int NOT NULL,
  `min_team_size` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKxmnatl52pxcdev1xnlbvxhuv` (`branch_id`),
  KEY `FKaw38g37ed9cphatb4li42viml` (`created_by`),
  CONSTRAINT `FKaw38g37ed9cphatb4li42viml` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`),
  CONSTRAINT `FKxmnatl52pxcdev1xnlbvxhuv` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_topics`
--

LOCK TABLES `project_topics` WRITE;
/*!40000 ALTER TABLE `project_topics` DISABLE KEYS */;
INSERT INTO `project_topics` VALUES (1,'category of the topic ','2026-09-07 03:35:16.814215','testing the discription of the topic ','ASSIGNED','tech stack should be java','test1 topic ',2,7,0,0),(2,'Artificial Intelligence','2026-09-07 03:35:36.176655','An automated attendance system using face recognition to identify students and record attendance.','AVAILABLE','Python, OpenCV, MySQL','AI-Based Attendance Systemmm',2,7,4,1),(3,'Web Development','2026-09-07 03:35:36.179240','A web-based system to manage books, students, issue and return records, and library transactions.','AVAILABLE','Java, Spring Boot, MySQL, React','Online Library Management System',2,7,2,1),(4,'Project Management','2026-09-07 03:35:36.181249','A platform for students to submit projects, faculty to review them, and HOD to approve or reject projects.','AVAILABLE','Java, Spring Boot, MySQL, HTML, CSS','Student Project Management System',2,7,3,2),(5,'Web Development','2026-09-07 03:35:36.182248','A system for managing student placement records, company drives, applications, and interview schedules.','AVAILABLE','Java, Spring Boot, MySQL, React','Online Placement Management System',2,7,5,3),(6,'FinTech','2026-09-07 03:35:36.183755','An application that allows users to record, categorize, and analyze their personal expenses.','AVAILABLE','Java, Spring Boot, MySQL, JavaScript','Smart Expense Tracker',2,7,7,1),(7,'lalalal','2026-09-11 19:07:54.966294','kakakak','ASSIGNED','lalala','kakakak',2,5,4,2);
/*!40000 ALTER TABLE `project_topics` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `projects`
--

DROP TABLE IF EXISTS `projects`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `projects` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(100) DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` text NOT NULL,
  `project_file_path` varchar(255) DEFAULT NULL,
  `project_type` enum('CUSTOM','TOPIC_BASED') NOT NULL,
  `rejected_at_stage` varchar(255) DEFAULT NULL,
  `report_file_path` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `tech_stack` varchar(300) DEFAULT NULL,
  `title` varchar(200) NOT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `branch_id` bigint NOT NULL,
  `student_id` bigint NOT NULL,
  `topic_id` bigint DEFAULT NULL,
  `faculty_guide_id` bigint DEFAULT NULL,
  `future_work` text,
  `github_url` varchar(500) DEFAULT NULL,
  `literature_review` text,
  `methodology` text,
  `objectives` text,
  `ppt_file_path` varchar(255) DEFAULT NULL,
  `problem_statement` text,
  `synopsis` text,
  `system_design` text,
  `video_url` varchar(500) DEFAULT NULL,
  `approved_team_size` int DEFAULT NULL,
  `requested_team_size` int DEFAULT NULL,
  `team_size_status` enum('APPROVED','CHANGED_BY_FACULTY','NOT_REVIEWED','PENDING_FACULTY_DECISION','REJECTED') DEFAULT NULL,
  `max_team_size` int DEFAULT NULL,
  `min_team_size` int DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKud6h1rm80igph4tedk5k1w6n` (`branch_id`),
  KEY `FKgotih2cqchnswrqacevuhjp0e` (`student_id`),
  KEY `FKtof5fc1uaumiq3g6e939rqjgs` (`topic_id`),
  KEY `FKdw0un4intnm1uom4u2y4p9mwp` (`faculty_guide_id`),
  CONSTRAINT `FKdw0un4intnm1uom4u2y4p9mwp` FOREIGN KEY (`faculty_guide_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKgotih2cqchnswrqacevuhjp0e` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FKtof5fc1uaumiq3g6e939rqjgs` FOREIGN KEY (`topic_id`) REFERENCES `project_topics` (`id`),
  CONSTRAINT `FKud6h1rm80igph4tedk5k1w6n` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=29 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `projects`
--

LOCK TABLES `projects` WRITE;
/*!40000 ALTER TABLE `projects` DISABLE KEYS */;
INSERT INTO `projects` VALUES (9,'category of the topic ','2026-09-09 04:04:15.584679','testing the discription of the topic ','projects/9/276dc851-fe14-411f-a52e-4d51559c2e46.zip','TOPIC_BASED',NULL,'reports/9/ec98c6d1-0287-4bcd-a76b-c6261791e139.pdf','COMPLETED','tech stack should be java','test1 topic ','2026-09-09 04:12:33.871257',2,6,1,10,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL),(17,'lalalal','2026-09-11 19:08:26.348082','kakakak','projects/17/eb1ea289-4891-471a-a16f-66cb20d54621.zip','TOPIC_BASED',NULL,'reports/17/b82b5f7a-5507-432c-b02f-4d7b5162e690.pdf','COMPLETED','lalala','kakakak','2026-09-15 01:31:02.649598',2,6,7,5,'gbrtbr','https://github.com/VikasChhetry/JON','brtbgr','rtbgr','fgrbr','proposals/17/840004ab-0585-40b9-8729-7fd91f63848e.pptx','fvgtrgr','dsfcef','brtb','https://www.youtube.com/watch?v=J1oAa4kVIDY',NULL,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `projects` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL,
  `email` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL,
  `role` varchar(255) DEFAULT NULL,
  `branch_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK9yy0ya980j002yvtxi9r7kv6b` (`branch_id`),
  CONSTRAINT `FK9yy0ya980j002yvtxi9r7kv6b` FOREIGN KEY (`branch_id`) REFERENCES `branch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (152,'chhetry.vk@gmail.com','Vikas','$2a$10$wCZ2sqfoaEf16CWOo5HBcO5e2VpTSbs8vjdQtMneZ6yGC2QH16QCO','STUDENT',NULL),(202,'student@gmail.com','Student1','$2a$10$yc8zqw6UNf9Bxee9nMwWE.HtyPbCAaiuH/LllOsJozNfkc0CEqo8a','STUDENT',NULL),(252,'student2@gmail.com','Student2','$2a$10$C.tPjHgNcuTuoStA2kWere52Ckmu4hcUXTa0KqmY1gUaKGUwfyj0W','STUDENT',NULL),(302,'Guide@gmail.com','Guide','$2a$10$giLgbCvEYI2B.q3aPL.Cx.OIL77Om8lljOIF0c81eqniD47jrGEC.','GUIDE',NULL),(352,'hod@gmail.com','Hod','$2a$10$B7WSv4dA/i5QbhY8CU3u1.iaRxPRACEnCezIyWY5CqkGd6NkdHZbS','HOD',1),(402,'hod1@gmail.com1','Hod1','$2a$10$8MVV6ySRZ.IrFlfM4hUALuVJ5pal5huEoprHvYb/QPDp3CCZRtccy','HOD',2),(403,'guide2@gmail.com','Guide2','$2a$10$P0k81unaX3oQGizgEAiIve1GnRxSowtYnMWbdzS4bs1GmpO5R8bzW','GUIDE',NULL),(452,'superadmin@gmail.com1','Super Admin','$2a$10$RLZ5f37cpjjVe76P.0PfEOTeK03kB5UE5RthupIa1icZPc6C0u9Ru','SUPER_ADMIN',NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_seq`
--

DROP TABLE IF EXISTS `user_seq`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_seq`
--

LOCK TABLES `user_seq` WRITE;
/*!40000 ALTER TABLE `user_seq` DISABLE KEYS */;
INSERT INTO `user_seq` VALUES (551);
/*!40000 ALTER TABLE `user_seq` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) NOT NULL,
  `email` varchar(150) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `full_name` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` enum('ADMIN','FACULTY','HOD','STUDENT') NOT NULL,
  `branch_id` bigint DEFAULT NULL,
  `max_guiding_capacity` int NOT NULL,
  `erp_id` varchar(50) DEFAULT NULL,
  `roll_number` varchar(50) DEFAULT NULL,
  `profile_photo_path` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK6dotkott2kjsp8vw4d0m25fb7` (`email`),
  KEY `FK9o70sp9ku40077y38fk4wieyk` (`branch_id`),
  CONSTRAINT `FK9o70sp9ku40077y38fk4wieyk` FOREIGN KEY (`branch_id`) REFERENCES `branches` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'2026-09-07 03:17:24.684926','chhetry.vk@gmail.com',_binary '','System Administrator','$2a$10$QVA.KwfEroH5vwRzQJWemeo2/exyw/Fr/WfumWnlj1Q8G25M6ubW6','ADMIN',NULL,0,NULL,NULL,NULL),(2,'2026-09-07 03:29:43.733399','vikas@gmail.com',_binary '','VIAKS CHHETRY','$2a$10$0v.ey0Brct.j0iu.tW1TKeLnST0JY6fHt3kXlahrSnuK2tCsVEVuC','STUDENT',1,0,NULL,NULL,NULL),(3,'2026-09-07 03:30:29.108267','ts2478093@gmail.com',_binary '','Faculty CSE ','$2a$10$/.oPDzsJelSN7W7RGlwh8.i9VMnhC1rIXkW1HTOYvR02qdyF0cpsa','FACULTY',1,2,NULL,NULL,NULL),(4,'2026-09-07 03:31:01.003843','vikasofficial097@gmail.com',_binary '','HOD CSE','$2a$10$ZbCvmQQprXNg24Fe7m7qiuWvFTrSo5zGaiQY07p8YJVVT.gTZJIgK','HOD',1,0,NULL,NULL,NULL),(5,'2026-09-07 03:32:10.589068','facultycs@gmail.com',_binary '','Faculty CS','$2a$10$.EtzlQADkeog6fNs1EHV3eT0LQ8aQYJ8NXpVx.sFDLbLWJB7MRJai','FACULTY',2,3,NULL,NULL,'profiles/1e1a174e-6890-4ddf-bfbb-9458be787d98.png'),(6,'2026-09-07 03:32:40.336900','0231cs094@niet.co.in',_binary '','Student CS','$2a$10$PltuC0NKvdEucWIubOjtHehl274dPEPfw4NGZfH0BgHNd5unQImiG','STUDENT',2,0,'0231cs094','2301330120115','profiles/9c6e3587-e979-4311-acfb-4691daafbeac.png'),(7,'2026-09-07 03:33:08.929026','hodcs@gmail.com',_binary '','HOD CS','$2a$10$PEGa..MsMhqe/MPhYKyU9.3cz9Q8Y6m8D7nDejgvKL30TdHKiQqZq','HOD',2,0,NULL,NULL,NULL),(8,'2026-09-07 21:59:32.415367','facultycs2@gmail.com',_binary '','Faculty CS2','$2a$10$5FuztgfVXn6GREqJMJlh.eIuxC4DcZUujGMb.Ihtv3MKu2KwAkcke','FACULTY',2,4,NULL,NULL,NULL),(9,'2026-09-07 21:59:54.530553','facultycs3@gmail.com',_binary '','Faculty CS3','$2a$10$uzsCwSA1nJUvJCDxLjKNqOW8Tz.lRwxE1z5m1mN1WJC1jh2lfrxce','FACULTY',2,2,NULL,NULL,NULL),(10,'2026-09-07 22:00:17.468254','facultycs4@gmail.com',_binary '','Faculty CS4','$2a$10$ZFMv5CrC.qP2UO/BpxKMUOXWWtDSWLnOt9S2639ctDDu4H504s8RG','FACULTY',2,1,NULL,NULL,NULL),(11,'2026-09-07 22:01:47.133784','studentcs2@gmail.com',_binary '','Student CS2','$2a$10$UTzJIE65.jYPJRbIeWq9/uwiJXQtbMh9mFDL7HHMc8POqgJBRWF5e','STUDENT',2,0,'0231cs091','2301330120091',NULL),(12,'2026-09-09 01:27:35.661897','admin2@example.com',_binary '','Admin User2','$2a$10$HUtGvlsOwttLt/FpnhJe1uvxHcK3ZVNkAB/ZAv1AErEArFFDhdzeO','ADMIN',2,0,NULL,NULL,NULL),(13,'2026-09-09 01:35:28.376697','john@example.com',_binary '','John Doe','$2a$10$lHvQxj6V3J6f1/njcpVuNe/WFYwqe4eeChHWAEagu75qEB0gFaeZa','STUDENT',2,0,NULL,NULL,NULL),(14,'2026-09-09 01:35:28.451052','jane@example.com',_binary '','Jane Smith','$2a$10$iJ/0ZFau7RluqKG5IzYhVOE40ROmn9wqacHB2X72WThy13pAsHAiO','FACULTY',4,0,NULL,NULL,NULL),(15,'2026-09-11 19:18:29.280210','student3cs@niet.co.in',_binary '','student3cs','$2a$10$mniFD6aq1jl4j6xweHhmPe1DINuk28g8..p0o3joQquyeOXf4Yuju','STUDENT',2,0,NULL,NULL,NULL),(16,'2026-09-11 19:21:52.927752','student8cs@niet.co.in',_binary '','student8cs','$2a$10$Khn9AWeCJ9Fk1Rc.M6J1SOyJzmZ.qli2FMI13j2P2rJH2ixtyByB2','STUDENT',2,0,NULL,NULL,NULL),(17,'2026-09-11 19:21:53.006894','student4cs@niet.co.in',_binary '','student4cs','$2a$10$ezgqyM9lteEYd9R/95/C9OUYk2LoEu86soTGAZPEicqb4NJ1Eb5wq','STUDENT',2,0,NULL,NULL,NULL),(18,'2026-09-11 19:21:53.088320','student5cs@niet.co.in',_binary '','student5cs','$2a$10$NmuMcseG/tPFmGYE9YAbTuvHJLnCrAAL33qQU3ANAsI8kUTFvJZz.','STUDENT',2,0,NULL,NULL,NULL),(19,'2026-09-11 19:21:53.164227','student6cs@niet.co.in',_binary '','student6cs','$2a$10$eG/ojFyjsnPp9cWVp/O5TeMCKgtTdv/cBRB9scyt0uMI6I576wEaS','STUDENT',2,0,NULL,NULL,NULL),(20,'2026-09-11 19:21:53.246362','student7cs@niet.co.in',_binary '','student7cs','$2a$10$oUtF4wSy0KB0HWNaSQPNZeH3u.SwRC2e7Oh1a/Y8E9jX1ciwvEAye','STUDENT',2,0,NULL,NULL,NULL),(21,'2026-09-15 02:02:13.351255','studentcs12@pas.com',_binary '','Student CS12','$2a$10$zFuEfuWcjFoCRXyye35A0O2.Lwr0eCmFReV8Omogsvi/v..f1W40K','STUDENT',2,0,NULL,NULL,NULL),(24,'2026-09-15 14:42:20.734692','admin@pas.com',_binary '','System Administrator','$2a$10$ghLTvjqqkaC2wfx4GCADh.YS2PJE80cDiIEHlee7gnq/2V4AmcoiC','ADMIN',NULL,0,NULL,NULL,NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-09-29  3:17:53
