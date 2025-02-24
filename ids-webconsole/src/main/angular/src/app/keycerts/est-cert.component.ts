import { Component, OnInit } from '@angular/core';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Title } from '@angular/platform-browser';
import { Router } from '@angular/router';

import { ESTService } from './est-service';
import { NGXLogger } from 'ngx-logger';

@Component({
    templateUrl: './est-cert.component.html'
})
export class ESTCertComponent implements OnInit {
    public myForm: UntypedFormGroup;
    public cacert = '';

    constructor(private readonly fb: UntypedFormBuilder,
                private readonly titleService: Title,
                private readonly log: NGXLogger,
                private readonly estService: ESTService,
                private readonly router: Router) {
        this.titleService.setTitle('Set EST CA cert');
    }

    public ngOnInit(): void {
        // the short way to create a FormGroup
        this.myForm = this.fb.group({
            estUrl: ['', Validators.required as any],
            certificateHash: '',
            certificate: ''
        });
    }

    public async fetchEstCert(): Promise<void> {
        await this.estService.requestEstCaCert(
            this.myForm.get('estUrl')?.value,
            this.myForm.get('certificateHash')?.value
        ).subscribe(e => {
            this.myForm.patchValue({
                certificate: e
            });
            this.cacert = e;
            this.log.error('Error during EST root certificate fetch', e);
        });
    }

    public async storeEstCert(): Promise<void> {
         await this.estService.uploadEstCaCert(this.cacert).subscribe(() => this.router.navigate(['/certificates']));
    }

}
