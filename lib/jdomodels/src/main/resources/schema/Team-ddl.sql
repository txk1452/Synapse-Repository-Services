CREATE TABLE IF NOT EXISTS `TEAM` (
  `ID` bigint(20) NOT NULL,
  `ETAG` char(36) NOT NULL,
  `PROPERTIES` mediumblob,
  PRIMARY KEY (`ID`),
  CONSTRAINT `TEAM_PRINCIPAL_FK` FOREIGN KEY (`ID`) REFERENCES `JDOUSERGROUP` (`ID`)
)
