/**
 * @copyright 2016 {@link http://infiniteautomation.com|Infinite Automation Systems, Inc.} All rights reserved.
 * @author Jared Wiltshire
 */

define(['require'], function(require) {
'use strict';

var dashboardMenuController = function dashboardMenuController() {
    this.childVisible = function childVisible(menuItems) {
        var visibleCount = 0;
        for (var i = 0; i < menuItems.length; i++) {
            var menuItem = menuItems[i];
            
            if (menuItem.children) {
                menuItem.visibleChildren = this.childVisible(menuItem.children);
                menuItem.visible = !menuItem.menuHidden && !!menuItem.visibleChildren && (!menuItem.permission || this.user.hasPermission(menuItem.permission));
            } else {
                menuItem.visible = !menuItem.menuHidden && (!menuItem.permission || this.user.hasPermission(menuItem.permission));
            }
            if (menuItem.visible) {
                visibleCount++;
            }
        }
        return visibleCount;
    };
    
    this.copyMenu = function() {
        if (!this.user || !this.origMenuItems) return;
        var items = angular.copy(this.origMenuItems);
        this.childVisible(items);
        this.menuItems = items;
    };
    
    this.$onChanges = function(changes) {
        this.copyMenu();
    };
    
    this.menuOpened = function menuOpened(toggleCtrl) {
        var submenu = null;
        var ctrl = toggleCtrl;
        while(ctrl) {
            if (ctrl.item.submenu) {
                submenu = ctrl.item;
                break;
            }
            ctrl = ctrl.parentToggle;
        }
        this.openSubmenu = submenu;
        
        this.openMenu = toggleCtrl.item;
        this.openMenuLevel = toggleCtrl.menuLevel + 1;
    };
    
    this.menuClosed = function menuOpened(toggleCtrl) {
        if (this.openSubmenu && toggleCtrl.item.name === this.openSubmenu.name) {
            delete this.openSubmenu;
        }
        if (this.openMenu && this.openMenu.name.indexOf(toggleCtrl.item.name) === 0) {
            this.openMenu = toggleCtrl.parentToggle ? toggleCtrl.parentToggle.item : null;
            this.openMenuLevel = toggleCtrl.menuLevel;
        }
    };
};

dashboardMenuController.$inject = [];

return {
    controller: dashboardMenuController,
    templateUrl: require.toUrl('./dashboardMenu.html'),
    bindings: {
        origMenuItems: '<menuItems',
        user: '<user'
    }
};

}); // define
