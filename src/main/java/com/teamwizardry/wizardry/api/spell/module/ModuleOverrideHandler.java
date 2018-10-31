package com.teamwizardry.wizardry.api.spell.module;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import com.teamwizardry.wizardry.api.spell.SpellRing;
import com.teamwizardry.wizardry.api.spell.annotation.ContextRing;
import com.teamwizardry.wizardry.api.util.ModuleClassUtils;

public class ModuleOverrideHandler {
	
	private HashMap<String, OverridePointer> overridePointers = new HashMap<>();
	private HashMap<String, Object> cachedProxies = new HashMap<>();

	public ModuleOverrideHandler(SpellRing spellRing) throws ModuleOverrideException {
		if( spellRing.getParentRing() != null )
			throw new IllegalArgumentException("passed spellRing is not a root.");
		
		SpellRing[] spellSequence = getSequenceFromSpellRing(spellRing);
		for( SpellRing curRing : spellSequence )
			applyOverrides(curRing);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getConsumerInterface(Class<T> interfaceClass) throws ModuleOverrideException {
		String className = interfaceClass.getName();
		Object obj = cachedProxies.get(className);
		if( obj == null ) {
			T newProxy = createConsumerInterface(interfaceClass);
			cachedProxies.put(className, newProxy);
			return newProxy;
		}
		
		// check for interface compatibility
		if( !interfaceClass.isInstance(obj) )
			throw new IllegalStateException("Incompatible interface class with matching name. Class loader different?");
		return (T)obj;
	}
	
	private <T> T createConsumerInterface(Class<T> interfaceClass) throws ModuleOverrideException {
		// Retrieve all overridable methods and check them for compatibility with base class
		Map<String, Method> overridableMethods = ModuleClassUtils.getOverridableModuleMethods(interfaceClass, true);
		
		// Create invocation handler. All interface methods are mapped to their base method pendants
		OverrideInvoker invocationHandler = new OverrideInvoker(overridableMethods);
		
		//
		ClassLoader myClassLoader = getClass().getClassLoader();	// Inherit class loader from this class
		Class<?>[] proxyInterfaces = new Class<?>[] { interfaceClass };
		
		@SuppressWarnings("unchecked")
		T proxy = (T)Proxy.newProxyInstance(myClassLoader, proxyInterfaces, invocationHandler);
		
		return proxy;
	}
	
	private void applyOverrides( SpellRing spellRing ) throws ModuleOverrideException {
		ModuleInstance module = spellRing.getModule();
		Map<String, Method> overrides = module.getFactory().getOverrides();
		
		for( Entry<String, Method> entry : overrides.entrySet() ) {
			OverridePointer ptr = overridePointers.get(entry.getKey());
			if( ptr == null ) {
				ptr = new OverridePointer(spellRing, null, module, entry.getKey(), entry.getValue());
			}
			else {
				if( !areMethodsCompatible(ptr.getBaseMethod(), entry.getValue()) )
					throw new ModuleOverrideException("Method '" + ptr.getBaseMethod() + "' can't be overridden by '" + entry.getValue() + "' due to incompatible signature.");
				ptr = new OverridePointer(spellRing,ptr, module, entry.getKey(), entry.getValue());
			}
			
			overridePointers.put(entry.getKey(), ptr);			
		}
	}
	
	/////////////////
	
	private static SpellRing[] getSequenceFromSpellRing(SpellRing spellRing) {
		SpellRing cur = spellRing;
		LinkedList<SpellRing> instances = new LinkedList<>();
		while( cur != null ) {
			ModuleInstance module = cur.getModule();
			if( !module.getFactory().hasOverrides() )
				continue;
			instances.add(cur);
			cur = cur.getChildRing();
		}
		
		return instances.toArray(new SpellRing[instances.size()]);
	}
	
	private static boolean areMethodsCompatible(Method baseMtd, Method overrideMtd) {
		// TODO: Move to reflection utils.
		
		// WARNING: Update this method, if language conventions in java change. 
		
		// Check compatibility of return types
		Class<?> baseReturnType = baseMtd.getReturnType();
		Class<?> overrideReturnType = overrideMtd.getReturnType();
		if( baseReturnType == null ) {
			if( overrideReturnType != null )
				return false;
		}
		else {
			if( overrideReturnType == null )
				return false;
			if( !baseReturnType.isAssignableFrom(overrideReturnType) )
				return false;
		}
		
		// Check compatibility of parameters
		int countParams = baseMtd.getParameterCount();
		Parameter[] baseParams = baseMtd.getParameters();
		Parameter[] overrideParams = overrideMtd.getParameters();

		int handledMethods = 0;
		for( int i = 0; i < countParams; i ++ ) {
			Parameter baseParam = baseParams[i];
			Parameter overrideParam = overrideParams[i];
			if( !baseParam.getType().isAssignableFrom(overrideParam.getType()) )
				return false;
			if( baseParam.isAnnotationPresent(ContextRing.class) )
				continue;	// Ignore parameters taking values from context
			
			handledMethods ++;
		}
		
		if( handledMethods != overrideMtd.getParameterCount() )
			return false;		
		
		// Check compatibility of exceptions
		Class<?>[] baseExcps = baseMtd.getExceptionTypes();
		Class<?>[] overrideExcps = overrideMtd.getExceptionTypes();
		
		// For every checked exception at the interface method
		// there should exist an exception type at base
		// which is assignable from the interface method exception
		// TODO: Maybe ignore unchecked exceptions?
		for( Class<?> overrideExcp : overrideExcps ) {
			boolean found = false;
			for( Class<?> baseExcp : baseExcps ) {
				if( baseExcp.isAssignableFrom(overrideExcp) ) {
					found = true;
					break;
				}
			}
			if( !found )
				return false;
		}

		return true;		
	}
	
	/////////////////
	
	private class OverrideInvoker implements InvocationHandler {
		private final HashMap<String, OverridePointer> callMap = new HashMap<>();
		private final int idxContextRing;
		
		public OverrideInvoker(Map<String, Method> overrides, int idxContextRing) throws ModuleOverrideException {
			this.idxContextRing = idxContextRing;
			
			for( Entry<String, Method> override : overrides.entrySet() ) {
				OverridePointer ptr = overridePointers.get(override.getKey());
				if( ptr == null )
					throw new ModuleOverrideException("Override with name '" + override.getKey() + "' referenced by '" + override.getValue()+"' is not existing.");
				callMap.put(override.getValue().getName(), ptr);
			}
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String name = method.getName();
			OverridePointer ptr = callMap.get(name);
			if( ptr == null )
				throw new UnsupportedOperationException("Override '" + name + "' for '" + method + "' is not existing.");
			
			Object passedArgs[] = args;
			if( idxContextRing >= 0 ) {
				// Add spell ring
				passedArgs = new Object[args.length + 1];
				int i = 0;
				int j = 0;
				while( i < args.length ) {
					if( i == idxContextRing ) {
						passedArgs[j] = ptr.getSpellRingWithOverride();
						j ++;
						continue;
					}
					passedArgs[j] = args[i];
					
					i ++;
					j ++;
				}
			}

			try {
				return ptr.getBaseMethod().invoke(ptr.getModule().getModuleClass(), passedArgs);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			} catch (IllegalAccessException | IllegalArgumentException e) {
				// NOTE: If this happens, then correctness of checks like "areMethodsCompatible()" a.s.o. need to be debugged.
				e.printStackTrace();
				throw new IllegalStateException("Couldn't invoke call. See cause.", e);
			}
		}
	}
	
	private static class OverridePointer {
		private final SpellRing spellRingWithOverride;
		private final String overrideName;
		private final Method baseMethod;
		private final ModuleInstance module;
		private final OverridePointer prev;
		
		OverridePointer(SpellRing spellRingWithOverride, OverridePointer prev, ModuleInstance module, String overrideName, Method baseMethod) {
			this.spellRingWithOverride = spellRingWithOverride;
			this.baseMethod = baseMethod;
			this.module = module;
			this.overrideName = overrideName;
			this.prev = prev;
		}
		
		SpellRing getSpellRingWithOverride() {
			return spellRingWithOverride;
		}
		
		Method getBaseMethod() {
			return this.baseMethod;
		}
		
		ModuleInstance getModule() {
			return module;
		}
		
		OverridePointer getPrev() {
			return prev;
		}
		
		String getOverrideName() {
			return overrideName;
		}
	}
}
