/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */
package com.caucho.ejb.timer;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.ScheduleExpression;
import javax.ejb.TimedObject;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

import com.caucho.config.types.CronType;
import com.caucho.config.types.Trigger;
import com.caucho.ejb.AbstractContext;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.resources.TimerTrigger;
import com.caucho.scheduling.CronExpression;
import com.caucho.scheduling.ScheduledTask;
import com.caucho.scheduling.Scheduler;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;

/**
 * Resin EJB timer service.
 */
// TODO This should probably be a application/server/cluster managed bean
// itself - would get rid of the boilerplate factory code; I could not figure
// out how to make that happen - tried @ApplicationScoped.
public class EjbTimerService implements TimerService {
  @SuppressWarnings("unused")
  private static final L10N L = new L10N(EjbTimerService.class);
  protected static final Logger log = Logger.getLogger(EjbTimerService.class
      .getName());

  private static final EnvironmentLocal<EjbTimerService> _localTimerService
    = new EnvironmentLocal<EjbTimerService>();

  private AbstractContext _context;

  /**
   * Creates a new timer service.
   *
   * @param context
   *          EJB context.
   */
  private EjbTimerService(AbstractContext context)
  {
    _context = context;
  }

  /**
   * Gets the local timer service for the class loader.
   *
   * @param loader
   *          Local class loader.
   * @param context
   *          EJB context.
   * @return Local timer service.
   */
  public static EjbTimerService getLocal(ClassLoader loader,
                                         AbstractContext context)
  {
    synchronized (_localTimerService) {
      EjbTimerService timerService = _localTimerService.get(loader);

      if (timerService == null) {
        timerService = new EjbTimerService(context);

        _localTimerService.set(timerService, loader);
      }

      return timerService;
    }
  }

  /**
   * Gets timer service for the current class loader.
   *
   * @return Timer service for the current class loader.
   */
  public static EjbTimerService getCurrent()
  {
    return getCurrent(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Gets the current timer service for a given class loader.
   *
   * @param loader
   *          Class loader for timer service.
   * @return The current timer service for the class loader.
   */
  public static EjbTimerService getCurrent(ClassLoader loader)
  {
    return _localTimerService.get(loader);
  }

  /**
   * Gets or creates the current timer service for a given class loader.
   *
   * @param loader
   *          Class loader for timer service.
   * @return The current timer service for the class loader.
   */
  public static EjbTimerService create()
  {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();

    synchronized (_localTimerService) {
      EjbTimerService service = _localTimerService.get();

      if (service == null) {
        service = new EjbTimerService(null);
        _localTimerService.set(service);
      }

      return service;
    }
  }

  /**
   * Create a single-action timer that expires after a specified duration.
   *
   * @param duration
   *          The number of milliseconds that must elapse before the timer
   *          expires.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration notification. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If duration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method fails due to a system-level failure.
   */
  @Override
  public Timer createTimer(long duration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (duration < 0) {
      throw new IllegalArgumentException("Timer duration must not be negative.");
    }

    Date expiration = new Date(Alarm.getCurrentTime() + duration);

    return createOneTimeTimer(expiration, info);
  }

  /**
   * Create a single-action timer that expires after a specified duration.
   *
   * @param duration
   *          The number of milliseconds that must elapse before the timer
   *          expires.
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If duration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method fails due to a system-level failure.
   */
  @Override
  public Timer createSingleActionTimer(long duration, TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (duration < 0) {
      throw new IllegalArgumentException("Timer duration must not be negative.");
    }

    Date expiration = new Date(Alarm.getCurrentTime() + duration);

    if (timerConfig != null) {
      return createOneTimeTimer(expiration, timerConfig.getInfo());
    } else {
      return createOneTimeTimer(expiration, null);
    }
  }

  /**
   * Create an interval timer whose first expiration occurs after a specified
   * duration, and whose subsequent expirations occur after a specified
   * interval.
   *
   * @param initialDuration
   *          The number of milliseconds that must elapse before the first timer
   *          expiration notification.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialDuration is negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createTimer(long initialDuration,
                           long intervalDuration,
                           Serializable info)
    throws IllegalArgumentException,
           IllegalStateException, EJBException
  {
    if (initialDuration < 0) {
      throw new IllegalArgumentException(
          "Timer initial duration must not be negative.");
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(
          "Timer interval duration must not be negative.");
    }

    Date expiration = new Date(Alarm.getCurrentTime() + initialDuration);

    return createRepeatingTimer(expiration, intervalDuration, info);
  }

  /**
   * Create an interval timer whose first expiration occurs after a specified
   * duration, and whose subsequent expirations occur after a specified
   * interval.
   *
   * @param initialDuration
   *          The number of milliseconds that must elapse before the first timer
   *          expiration notification.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialDuration is negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createIntervalTimer(long initialDuration,
                                   long intervalDuration,
                                   TimerConfig timerConfig)
    throws IllegalArgumentException,
           IllegalStateException, EJBException
  {
    if (initialDuration < 0) {
      throw new IllegalArgumentException(
          "Timer initial duration must not be negative.");
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(
          "Timer interval duration must not be negative.");
    }

    Date expiration = new Date(Alarm.getCurrentTime() + initialDuration);

    if (timerConfig != null) {
      return createRepeatingTimer(expiration, intervalDuration, timerConfig
          .getInfo());
    } else {
      return createRepeatingTimer(expiration, intervalDuration, null);
    }
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   *
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration notification. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If expiration is null, or expiration.getTime() is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createTimer(Date expiration, Serializable info)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (expiration == null) {
      throw new IllegalArgumentException("Timer expiration must not be null.");
    }

    if (expiration.getTime() < 0) {
      throw new IllegalArgumentException(
          "Timer expiration must not be negative.");
    }

    return createOneTimeTimer(expiration, info);
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   *
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If expiration is null, or expiration.getTime() is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createSingleActionTimer(Date expiration,
                                       TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (expiration == null) {
      throw new IllegalArgumentException("Timer expiration must not be null.");
    }

    if (expiration.getTime() < 0) {
      throw new IllegalArgumentException(
          "Timer expiration must not be negative.");
    }

    if (timerConfig != null) {
      return createOneTimeTimer(expiration, timerConfig.getInfo());
    } else {
      return createOneTimeTimer(expiration, null);
    }
  }

  /**
   * Create an interval timer whose first expiration occurs at a given point in
   * time and whose subsequent expirations occur after a specified interval.
   *
   * @param initialExpiration
   *          The point in time at which the first timer expiration must occur.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialExpiration is null, or initialExpiration.getTime() is
   *           negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createTimer(Date initialExpiration,
                           long intervalDuration,
                           Serializable info)
    throws IllegalArgumentException,
           IllegalStateException, EJBException
  {
    if (initialExpiration == null) {
      throw new IllegalArgumentException(
          "Timer initial expiration must not be null.");
    }

    if (initialExpiration.getTime() < 0) {
      throw new IllegalArgumentException(
          "Timer initial expiration must not be negative.");
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(
          "Timer interval duration must not be negative.");
    }

    return createRepeatingTimer(initialExpiration, intervalDuration, info);
  }

  /**
   * Create an interval timer whose first expiration occurs at a given point in
   * time and whose subsequent expirations occur after a specified interval.
   *
   * @param initialExpiration
   *          The point in time at which the first timer expiration must occur.
   * @param intervalDuration
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications. Expiration notifications are scheduled
   *          relative to the time of the first expiration. If expiration is
   *          delayed (e.g. due to the interleaving of other method calls on the
   *          bean) two or more expiration notifications may occur in close
   *          succession to "catch up".
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If initialExpiration is null, or initialExpiration.getTime() is
   *           negative, or intervalDuration is negative.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createIntervalTimer(Date initialExpiration,
                                   long intervalDuration,
                                   TimerConfig timerConfig)
      throws IllegalArgumentException, IllegalStateException, EJBException
  {
    if (initialExpiration == null) {
      throw new IllegalArgumentException(
          "Timer initial expiration must not be null.");
    }

    if (initialExpiration.getTime() < 0) {
      throw new IllegalArgumentException(
          "Timer initial expiration must not be negative.");
    }

    if (intervalDuration < 0) {
      throw new IllegalArgumentException(
          "Timer interval duration must not be negative.");
    }

    if (timerConfig != null) {
      return createRepeatingTimer(initialExpiration, intervalDuration,
          timerConfig.getInfo());
    } else {
      return createRepeatingTimer(initialExpiration, intervalDuration, null);
    }
  }

  /**
   * Create a calendar-based timer based on the input schedule expression.
   *
   * @param schedule
   *          A schedule expression describing the timeouts for this timer.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If Schedule represents an invalid schedule expression.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createCalendarTimer(ScheduleExpression schedule,
      Serializable info) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    return createScheduledTimer(schedule, info);
  }

  /**
   * Create a calendar-based timer based on the input schedule expression.
   *
   * @param schedule
   *          A schedule expression describing the timeouts for this timer.
   * @param timerConfig
   *          Timer configuration.
   * @return The newly created Timer.
   * @throws IllegalArgumentException
   *           If Schedule represents an invalid schedule expression.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @Override
  public Timer createCalendarTimer(ScheduleExpression schedule,
      TimerConfig timerConfig) throws IllegalArgumentException,
      IllegalStateException, EJBException
  {
    if (timerConfig != null) {
      return createScheduledTimer(schedule, timerConfig.getInfo());
    } else {
      return createScheduledTimer(schedule, null);
    }
  }

  /**
   * Get all the active timers associated with this bean.
   *
   * @return A collection of javax.ejb.Timer objects.
   * @throws IllegalStateException
   *           If this method is invoked while the instance is in a state that
   *           does not allow access to this method.
   * @throws EJBException
   *           If this method could not complete due to a system-level failure.
   */
  @SuppressWarnings("unchecked")
  @Override
  public Collection<Timer> getTimers() throws IllegalStateException,
      EJBException
  {
    Collection<Timer> timers = new LinkedList<Timer>();

    // TODO I'm not sure this is right; what's really needed here is a way to
    // uniquely identify a bean whether it is an EJB or not. Maybe
    // getDeployBean is more appropriate and is uniquely identifiable in JCDI,
    // including EJBs?
    Class invokingBean = _context.getServer().getAnnotatedType().getJavaClass();

    Collection<ScheduledTask> scheduledTasks
      = Scheduler.getScheduledTasksByTargetBean(invokingBean);

    for (ScheduledTask scheduledTask : scheduledTasks) {
      EjbTimer timer = new EjbTimer();
      timer.setScheduledTask(scheduledTask);

      timers.add(timer);
    }

    return timers;
  }

  /**
   * Create a single-action timer that expires at a given point in time.
   *
   * @param expiration
   *          The point in time at which the timer must expire.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  @SuppressWarnings("unchecked")
  private Timer createOneTimeTimer(Date expiration, Serializable info)
  {
    Trigger trigger = new TimerTrigger(expiration.getTime());
    Class targetBean = getTargetBean();
    Method targetMethod = getTargetMethod(targetBean);
    EjbTimer timer = new EjbTimer();
    ScheduledTask scheduledTask = new ScheduledTask(targetBean, targetMethod,
        timer, null, trigger, -1, -1, info);
    timer.setScheduledTask(scheduledTask);

    // TODO This should probably be an injection of the scheduler by JCDI.
    Scheduler.addScheduledTask(scheduledTask);

    return timer;
  }

  /**
   * Create an interval timer whose first expiration occurs at a given point in
   * time and whose subsequent expirations occur after a specified interval.
   *
   * @param expiration
   *          The point in time at which the first timer expiration must occur.
   * @param interval
   *          The number of milliseconds that must elapse between timer
   *          expiration notifications.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  @SuppressWarnings("unchecked")
  private Timer createRepeatingTimer(Date expiration, long interval,
      Serializable info)
  {
    Trigger trigger = new TimerTrigger(expiration.getTime(), interval);
    Class targetBean = getTargetBean();
    Method targetMethod = getTargetMethod(targetBean);
    EjbTimer timer = new EjbTimer();
    ScheduledTask scheduledTask = new ScheduledTask(targetBean, targetMethod,
        timer, null, trigger, -1, -1, info);
    timer.setScheduledTask(scheduledTask);

    // TODO This should probably be an injection of the scheduler by JCDI.
    Scheduler.addScheduledTask(scheduledTask);

    return timer;
  }

  /**
   * Create a calendar-based timer based on the input schedule expression.
   *
   * @param schedule
   *          A schedule expression describing the timeouts for this timer.
   * @param info
   *          Application information to be delivered along with the timer
   *          expiration. This can be null.
   * @return The newly created Timer.
   */
  @SuppressWarnings("unchecked")
  private Timer createScheduledTimer(ScheduleExpression schedule,
      Serializable info)
  {
    CronExpression cronExpression = new CronExpression(schedule.getSecond(),
        schedule.getMinute(), schedule.getHour(), schedule.getDayOfWeek(),
        schedule.getDayOfMonth(), schedule.getMonth(), schedule.getYear());
    Trigger trigger = new CronType(schedule.getSecond(), schedule.getMinute(),
        schedule.getHour(), schedule.getDayOfWeek(), schedule.getDayOfMonth(),
        schedule.getMonth(), schedule.getYear(), schedule.getStart(), schedule
            .getEnd());
    Class targetBean = getTargetBean();
    Method targetMethod = getTargetMethod(targetBean);
    EjbTimer timer = new EjbTimer();
    ScheduledTask scheduledTask = new ScheduledTask(targetBean, targetMethod,
        timer, cronExpression, trigger, schedule.getStart().getTime(), schedule
            .getEnd().getTime(), info);
    timer.setScheduledTask(scheduledTask);

    // TODO This should probably be an injection of the scheduler by JCDI.
    Scheduler.addScheduledTask(scheduledTask);

    return timer;
  }

  // TODO I'm not sure this is right; what's really needed here is a way to
  // uniquely identify a bean whether it is an EJB or not. Maybe
  // getDeployBean is more appropriate and is uniquely identifiable in JCDI,
  // including EJBs?
  @SuppressWarnings("unchecked")
  private Class getTargetBean()
  {
    return _context.getServer().getAnnotatedType().getJavaClass();
  }

  @SuppressWarnings("unchecked")
  private Method getTargetMethod(Class targetBean) throws EJBException
  {
    Method targetMethod = null;

    if (!TimedObject.class.isAssignableFrom(targetBean)) {
      for (Method method : targetBean.getMethods()) {
        if (method.getAnnotation(Timeout.class) != null) {
          targetMethod = method;
        }
      }

      if (targetMethod == null) {
        throw new EJBException(
            "No timeout method to be invoked by the timer found.");
      }
    }

    return targetMethod;
  }

  /**
   * Returns a string representation of the object.
   *
   * @return String representation of the object.
   */
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}