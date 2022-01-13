package org.prowl.themeexampleplugin.annotations;

/**
 * This is just a simple annotation that we use with the r8 to tell it we don't want methods tagged
 * with this in a release build - handy for removing debug!
 */
public @interface RemovableInRelease {

}
