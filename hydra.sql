--
-- Hydra WebManager Database
--

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Database: `hydra`
--

-- --------------------------------------------------------

--
-- table `nodes`
--

CREATE TABLE IF NOT EXISTS `nodes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `slave` int(11) DEFAULT NULL,
  `group` int(11) DEFAULT NULL,
  `assigned_slave` int(11) DEFAULT NULL,
  `session` int(11) NOT NULL,
  `name` varchar(256) DEFAULT NULL,
  `state` enum('draft','scheduled','created','connected','destroyed','error') NOT NULL DEFAULT 'draft',
  `address` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `address` (`address`),
  KEY `group` (`group`),
  KEY `session` (`session`),
  KEY `session_slave` (`session`,`slave`),
  KEY `assigned_slave` (`assigned_slave`),
  KEY `state` (`state`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- table `sessions`
--

CREATE TABLE IF NOT EXISTS `sessions` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user` int(11) NOT NULL,
  `name` varchar(256) DEFAULT NULL,
  `created` datetime NOT NULL,
  `started` datetime DEFAULT NULL,
  `aborted` datetime DEFAULT NULL,
  `finished` datetime DEFAULT NULL,
  `state` enum('initial','draft','pending','running','finished','aborted','error','cancelled') NOT NULL DEFAULT 'initial',
  PRIMARY KEY (`id`),
  KEY `user` (`user`),
  KEY `name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- table `slaves`
--

CREATE TABLE IF NOT EXISTS `slaves` (
  `Id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `address` varchar(64) DEFAULT NULL,
  `state` enum('disconnected','connected') NOT NULL DEFAULT 'disconnected',
  `owner` int(11) DEFAULT NULL,
  `capacity` int(11) DEFAULT NULL,
  PRIMARY KEY (`Id`),
  UNIQUE KEY `name_address` (`name`,`address`),
  KEY `owner` (`owner`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- table `stats`
--

CREATE TABLE IF NOT EXISTS `stats` (
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `session` int(11) NOT NULL,
  `node` int(11) NOT NULL,
  `data` text NOT NULL,
  UNIQUE KEY `timestamp` (`timestamp`,`node`),
  KEY `node` (`node`),
  KEY `session` (`session`),
  KEY `session_node` (`session`,`node`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- table `users`
--

CREATE TABLE IF NOT EXISTS `users` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL,
  `passwd` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB  DEFAULT CHARSET=latin1;

--
-- data for table `users`
--

INSERT INTO `users` (`id`, `name`, `passwd`) VALUES
(1, 'admin', NULL);

--
-- table `usersessions`
--

CREATE TABLE IF NOT EXISTS `usersessions` (
`id` int(11) NOT NULL,
  `username` varchar(64) NOT NULL,
  `sessionid` varchar(256) DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=5 ;

--
-- Indexes for table `usersessions`
--
ALTER TABLE `usersessions`
 ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for table `usersessions`
--
ALTER TABLE `usersessions`
MODIFY `id` int(11) NOT NULL AUTO_INCREMENT,AUTO_INCREMENT=5;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
