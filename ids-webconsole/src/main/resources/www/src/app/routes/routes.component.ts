import { Component, OnInit, OnDestroy, EventEmitter, Output } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { FormsModule }   from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { IntervalObservable } from "rxjs/observable/IntervalObservable";
import 'rxjs/add/operator/takeWhile';

import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription'

import { Route } from './route';
import { RouteService } from './route.service';
import { RouteMetrics } from './route-metrics';


@Component({
    selector: 'route-list',
    templateUrl: './routes.component.html',
})

export class RoutesComponent implements OnInit, OnDestroy {
    title = 'Current Routes';
    routes: Route[];
    selectedRoute: Route;
    routemetrics: RouteMetrics = new RouteMetrics();
    private alive: boolean;

    @Output() changeTitle = new EventEmitter();

    constructor(private titleService: Title, private routeService: RouteService) {
        this.titleService.setTitle('Message Routes');

        this.routeService.getRoutes().subscribe(routes => {
            this.routes = routes;
        });
    }

    ngOnInit(): void {
        this.changeTitle.emit('Camel Routes');
        // Update route metrics every 1s
        IntervalObservable.create(2000)
            .takeWhile(() => this.alive )
            .flatMap(() => { 
                return this.routeService.getMetrics();
               })
            .subscribe(res => this.routemetrics = res);
        this.alive = true;
//        
//        this.timerSubscription = Observable.timer(0, 1000).subscribe(() => {
//            this.routeService.getMetrics().subscribe(result => this.routemetrics = result);
//        });
    }

    ngOnDestroy() {
        this.alive = false;
    }

    onSelect(route: Route): void {
        this.selectedRoute = route;
    }
}
