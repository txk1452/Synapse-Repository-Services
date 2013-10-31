CREATE TABLE `MESSAGE_STATUS` (
  `MESSAGE_STATUS_ID` bigint(20) NOT NULL,
  `RECIPIENT_ID` bigint(20) NOT NULL,
  `STATUS` ENUM('READ', 'UNREAD', 'ARCHIVED') NOT NULL, 
  PRIMARY KEY (`MESSAGE_STATUS_ID`, `RECIPIENT_ID`),
  CONSTRAINT `MESSAGE_ID_FK` FOREIGN KEY (`MESSAGE_STATUS_ID`) REFERENCES `MESSAGE` (`MESSAGE_ID`) ON DELETE CASCADE,
  CONSTRAINT `RECIPIENT_ID_FK` FOREIGN KEY (`RECIPIENT_ID`) REFERENCES `JDOUSERGROUP` (`ID`) ON DELETE CASCADE
)