/*
 ** Copyright (C) 2013 Mellanox Technologies
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at:
 **
 ** http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 ** either express or implied. See the License for the specific language
 ** governing permissions and limitations under the License.
 **
 */
package com.mellanox.jxio;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** 
 * Reason for SessionEvent as received from Accelio
 */
public enum EventReason {

	SUCCESS(0),

	EPERM(1),
	ENOENT(2),
	ESRCH(3),
	EINTR(4),
	EIO(5),
	ENXIO(6),
	E2BIG(7),
	ENOEXEC(8),
	EBADF(9),
	ECHILD(10),
	EDEADLK(45),
	ENOMEM(12),
	EACCES(13),
	EFAULT(14),
	ENOTBLK(15),
	EBUSY(16),
	EEXIST(17),
	EXDEV(18),
	ENODEV(19),
	ENOTDIR(20),
	EISDIR(21),
	EINVAL(22),
	ENFILE(23),
	EMFILE(24),
	ENOTTY(25),
	ETXTBSY(26),
	EFBIG(27),
	ENOSPC(28),
	ESPIPE(29),
	EROFS(30),
	EMLINK(31),
	EPIPE(32),
	EDOM(33),
	ERANGE(34),
	EWOULDBLOCK(11),
	EAGAIN(11),
	EINPROGRESS(150),
	EALREADY(149),
	ENOTSOCK(95),
	EDESTADDRREQ(96),
	EMSGSIZE(97),
	EPROTOTYPE(98),
	ENOPROTOOPT(99),
	EPROTONOSUPPORT(120),
	ESOCKTNOSUPPORT(121),
	EOPNOTSUPP(122),
	EPFNOSUPPORT(123),
	EAFNOSUPPORT(124),
	EADDRINUSE(125),
	EADDRNOTAVAIL(126),
	ENETDOWN(127),
	ENETUNREACH(128),
	ENETRESET(129),
	ECONNABORTED(130),
	ECONNRESET(131),
	ENOBUFS(132),
	EISCONN(133),
	ENOTCONN(134),
	ESHUTDOWN(143),
	ETOOMANYREFS(144),
	ETIMEDOUT(145),
	ECONNREFUSED(146),
	ELOOP(90),
	ENAMETOOLONG(78),
	EHOSTDOWN(147),
	EHOSTUNREACH(148),
	ENOTEMPTY(93),
	EUSERS(94),
	EDQUOT(49),
	ESTALE(151),
	EREMOTE(66),
	ENOLCK(46),
	ENOSYS(89),
	EOVERFLOW(79),
	EIDRM(36),
	ENOMSG(35),
	EILSEQ(88),
	EBADMSG(77),
	EMULTIHOP(74),
	ENODATA(61),
	ENOLINK(67),
	ENOSR(63),
	ENOSTR(60),
	EPROTO(71),
	ETIME(62),

	JXIO_GENERAL_ERROR(1247689300),
	NOT_SUPPORTED(JXIO_GENERAL_ERROR.getIndex() + 1),
	NO_BUFS(JXIO_GENERAL_ERROR.getIndex() + 2),
	CONNECT_ERROR(JXIO_GENERAL_ERROR.getIndex() + 3),
	ROUTE_ERROR(JXIO_GENERAL_ERROR.getIndex() + 4),
	ADDR_ERROR(JXIO_GENERAL_ERROR.getIndex() + 5),
	UNREACHABLE(JXIO_GENERAL_ERROR.getIndex() + 6),
	MSG_SIZE(JXIO_GENERAL_ERROR.getIndex() + 7),
	PARTIAL_MSG(JXIO_GENERAL_ERROR.getIndex() + 8),
	MSG_INVALID(JXIO_GENERAL_ERROR.getIndex() + 9),
	MSG_UNKNOWN(JXIO_GENERAL_ERROR.getIndex() + 10),
	SESSION_REFUSED(JXIO_GENERAL_ERROR.getIndex() + 11),
	SESSION_ABORTED(JXIO_GENERAL_ERROR.getIndex() + 12),
	SESSION_DISCONNECTED(JXIO_GENERAL_ERROR.getIndex() + 13),
	SESSION_REJECTED(JXIO_GENERAL_ERROR.getIndex() + 14),
	SESSION_REDIRECTED(JXIO_GENERAL_ERROR.getIndex() + 15),
	BIND_FAILED(JXIO_GENERAL_ERROR.getIndex() + 16),
	TIMEOUT(JXIO_GENERAL_ERROR.getIndex() + 17),
	IN_PROGRESS(JXIO_GENERAL_ERROR.getIndex() + 18),
	INVALID_VERSION(JXIO_GENERAL_ERROR.getIndex() + 19),
	NOT_SESSION(JXIO_GENERAL_ERROR.getIndex() + 20),
	OPEN_FAILED(JXIO_GENERAL_ERROR.getIndex() + 21),
	READ_FAILED(JXIO_GENERAL_ERROR.getIndex() + 22),
	WRITE_FAILED(JXIO_GENERAL_ERROR.getIndex() + 23),
	CLOSE_FAILED(JXIO_GENERAL_ERROR.getIndex() + 24),
	UNSUCCESSFUL(JXIO_GENERAL_ERROR.getIndex() + 25),
	MSG_CANCELED(JXIO_GENERAL_ERROR.getIndex() + 26),
	MSG_CANCEL_FAILED(JXIO_GENERAL_ERROR.getIndex() + 27),
	MSG_NOT_FOUND(JXIO_GENERAL_ERROR.getIndex() + 28),
	MSG_FLUSHED(JXIO_GENERAL_ERROR.getIndex() + 29),
	MSG_DISCARDED(JXIO_GENERAL_ERROR.getIndex() + 30),
	STATE(JXIO_GENERAL_ERROR.getIndex() + 31),
	NO_USER_BUFS(JXIO_GENERAL_ERROR.getIndex() + 32),
	NO_USER_MR(JXIO_GENERAL_ERROR.getIndex() + 33),
	USER_BUF_OVERFLOW(JXIO_GENERAL_ERROR.getIndex() + 34),
	REM_USER_BUF_OVERFLOW(JXIO_GENERAL_ERROR.getIndex() + 35);

	private int index;

	private EventReason(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}

	private static final Log LOG = LogFactory.getLog(EventReason.class.getCanonicalName());

	private static final Map<Integer, EventReason> eventReasonMap = new HashMap<Integer, EventReason>();
	static {
		for (EventReason eventReason : EventReason.values()) {
			eventReasonMap.put(eventReason.getIndex(), eventReason);
		}
	}

	public static EventReason getEventByXioIndex(int xioIndex) {
		EventReason returnEventReason = eventReasonMap.get(xioIndex);
		if (returnEventReason == null) {
			LOG.warn("Unmapped XIO event index = '" + xioIndex + "'. Returning with JXIO_GENERAL_ERROR");
			return JXIO_GENERAL_ERROR;
		}
		return returnEventReason;
	}
}