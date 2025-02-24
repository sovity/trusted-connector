import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { ESTService } from './est-service';
import { EstEnrollment } from './est-enrollment.interface';

@Component({
    templateUrl: './identitynewest.component.html'
})
export class NewIdentityESTComponent implements OnInit {
    public myForm: UntypedFormGroup;

    constructor(private readonly fb: UntypedFormBuilder,
                private readonly titleService: Title,
                private readonly estService: ESTService,
                private readonly router: Router) {
        this.titleService.setTitle('New Identity via EST Enrollment');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            estUrl: '',
            iet: '',
            alias: '1'
        });
    }

    public save(data: EstEnrollment): void {
       // Call REST to create identity
       this.estService.createIdentity(data).subscribe(() => this.router.navigate(['/certificates']));
    }
}
