package me.cheat.client.events;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Event {
    private boolean cancelled;
    private Era era = Era.PRE;

    public enum Era {
        PRE, POST
    }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public Era getEra() { return era; }
    public void setEra(Era era) { this.era = era; }

    public static class EventBus {
        private static final Map<Class<?>, List<Listener>> listeners = new ConcurrentHashMap<>();
        private static final CopyOnWriteArrayList<Object> registered = new CopyOnWriteArrayList<>();

        public static void register(Object object) {
            if (registered.contains(object)) return;
            registered.add(object);
            
            for (java.lang.reflect.Method method : object.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(EventTarget.class) && method.getParameterCount() == 1) {
                    method.setAccessible(true);
                    Class<?> eventClass = method.getParameterTypes()[0];
                    if (Event.class.isAssignableFrom(eventClass)) {
                        listeners.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>())
                                .add(new Listener(object, method));
                    }
                }
            }
        }

        public static void unregister(Object object) {
            registered.remove(object);
            for (List<Listener> list : listeners.values()) {
                list.removeIf(l -> l.instance == object);
            }
        }

        public static void call(Event event) {
            List<Listener> list = listeners.get(event.getClass());
            if (list != null) {
                for (Listener listener : list) {
                    try {
                        listener.method.invoke(listener.instance, event);
                    } catch (InvocationTargetException e) {
                        if (e.getCause() != null) {
                            e.getCause().printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private static class Listener {
            final Object instance;
            final java.lang.reflect.Method method;

            Listener(Object instance, java.lang.reflect.Method method) {
                this.instance = instance;
                this.method = method;
            }
        }
    }
}
