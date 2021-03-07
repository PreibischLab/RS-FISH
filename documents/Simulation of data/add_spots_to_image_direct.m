function [stack2,p] = add_spots_to_image_direct(stack,varargin)
%function that adds 'nspots' diffraction limited spots 
%to an existing image 'stack'

%% output
%stack2: the 3d or 2d image with the spots added
%p: the structure holding the spots parameters
    %see parameters definition in function
    %init_spot_generation_parameters_struct

%% input
%stack: the original image
%stack,save_dirname: the original image and the saving directory    

if isempty(varargin)
    save_dirname = uigetdir('Select Saving Directory');
else
    save_dirname = varargin{1};
end


%%%%%%%%%%%%%%%%%%%%%% spot parameters %%%%%%%%%%%%%%%%%%%%%%%%%

p = struct('npts',{},'bg_mean',{},'bg_sd',{},'dlocpix',{},'imsize',{},'maxbounds',{},...
         'brightness',{},'vox_size',{},'psf',{},'cutsize',{},'gain',{});
     
    %number of spots in the image
    p(1).npts = 30;
    
    %average background value
    p.bg_mean = 200;
    
    %standard deviation of the background noise (gaussian distributed)
    p.bg_sd = 8;
    
    %use this to set determinsitic spot positions, leave empty for random
    %locations
    %p.dlocpix = npts; 
    %with the format: p.dlocpix = [x y z I]
    p.dlocpix = [];
     
    %brightness of the spots ([mean,sd])
    p.brightness = [300 0];
    
    %size of the voxel in physical units (optional)
    p.vox_size = [1 1 1];
    
    %std dev of the PSF along xy and z respectively in pixels
    p.psf = [2 2];
    
    %shape of the z profile (gaussian vs integrated gaussian)
    %p.zmode ='integrated gaussian';
    p.zmode ='gaussian';
    
    %size of the region around the spot where intensity is caluclated in PSF std dev units
    %ROI is a cube centered on the spot with side 2*p.cutsize+1
    p.cutsize = 3;
    
    %gain of the camera
    p.gain = 1;
    
    %intensity calculation
    p.mode = 'poisson';   %intensity is computed as a poisson distributed value (shot noise)
    %p.mode = ''; %determinstic intensity from the PSF function

    %array [xmin ymin zmin;xmax ymax zmax] that specifies the range in
    %pix of the region where the the spots are allowed to be positioned 
    %(further restricted by subtratcting 3 psf radii in each limit)
    p.maxbounds = 0;
    
    %image size
    p.imsize = size(stack);
    
    
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%    
     
%getting the spots positions (in pix units) if they need to be defined
if size(p.dlocpix,2) == 1 || isempty(p.dlocpix)
    p.dlocpix = generate_spots_positions_and_intensities(stack,p.npts,p.maxbounds,p.brightness,p.vox_size,p.psf);
end

%generating an image stack2 that is similar to stack and adding the spots to it
stack2 = double(stack) + p.bg_mean + p.bg_sd*randn(size(stack));

if ndims(stack) == 3
    if size(p.dlocpix,2) ~= 4
        disp('dlocpix array should have 4 columns');
        return;
    end
    [nx,ny,nz] = size(stack);

    for j=1 : size(p.dlocpix,1)

                xdot = p.dlocpix(j,1);
                ydot = p.dlocpix(j,2);
                zdot = p.dlocpix(j,3);

                xmin = xdot - p.cutsize*p.psf(1)/p.vox_size(1); %defining the ensemble of pixels b/w xmin:xmax ymin:ymax etc
                xmax = xdot + p.cutsize*p.psf(1)/p.vox_size(1); %these are the pixels on which the current dot is
                ymin = ydot - p.cutsize*p.psf(1)/p.vox_size(1); %gonna contribute non-negligible intensity
                ymax = ydot + p.cutsize*p.psf(1)/p.vox_size(1); %they are defined as pixels within a 3-sigma radius from the dot
                zmin = zdot - p.cutsize*p.psf(2)/p.vox_size(2);
                zmax = zdot + p.cutsize*p.psf(2)/p.vox_size(2);

                xp_min = max( ceil(xmin), 1);
                xp_max = min( ceil(xmax), nx );
                yp_min = max( ceil(ymin), 1);
                yp_max = min( ceil(ymax), ny );
                zp_min = max( ceil(zmin), 1);
                zp_max = min( ceil(zmax), nz );
                
                [xp,yp,zp] = ndgrid(xp_min:xp_max,yp_min:yp_max,zp_min:zp_max);
                
                diffx1 =  (double(xp-1))*p.vox_size(1) - xdot*p.vox_size(1);
                diffx1 = diffx1 / ( sqrt(2.0)*p.psf(1));
                diffx2 =  (double(xp))*p.vox_size(1) - xdot*p.vox_size(1);
                diffx2 = diffx2 / ( sqrt(2.0)*p.psf(1));

                diffy1 =  (double(yp-1))*p.vox_size(2) - ydot*p.vox_size(2);
                diffy1 = diffy1 / ( sqrt(2.0)*p.psf(1) );
                diffy2 =  (double(yp))*p.vox_size(2) - ydot*p.vox_size(2);
                diffy2 = diffy2 / ( sqrt(2.0)*p.psf(1) );
                
                if strcmp(p.zmode,'integrated gaussian') 
                    diffz1 =  (double(zp-1))*p.vox_size(3) - zdot*p.vox_size(3);
                    diffz1 = diffz1 / ( sqrt(2.0)*p.psf(2) );
                    diffz2 =  (double(zp))*p.vox_size(3) - zdot*p.vox_size(3);
                    diffz2 = diffz2 / ( sqrt(2.0)*p.psf(2) );

                    intensity = abs( erf( diffx1) - erf(diffx2) ).* abs( erf( diffy1) - erf(diffy2) ).* abs( erf( diffz1) - erf(diffz2)  );
                    intensity = intensity / 8.0;
                elseif strcmp(p.zmode,'gaussian') 
                    gz = exp( - ( double(zp) *p.vox_size(3) - zdot*p.vox_size(3)).^2 /(2*p.psf(2)^2));
                    intensity = abs( erf( diffx1) - erf(diffx2) ) .* abs( erf( diffy1) - erf(diffy2) ).* gz;
                    intensity = intensity / (sqrt(pi)*4.0);
                end
                
                intensity = p.dlocpix(j,4)*intensity;
                
                %loop over the non-negligible intensity pixels
                %compute first the difference b/w the edges of the pixel and
                %the dot. then evaluate the integral of the gaussian PSF over the pixel.
                
                if strcmp(p.mode,'poisson')
                               %taking a poisson distribution
                   intensity = p.gain*poissrnd(intensity/p.gain);
                end
                stack2(xp_min:xp_max,yp_min:yp_max,zp_min:zp_max) = stack2(xp_min:xp_max,yp_min:yp_max,zp_min:zp_max) + intensity;
                clear('intensity');
    end
    
elseif ndims(stack) == 2
    if size(p.dlocpix,2) ~= 3
        disp('dlocpix array should have 3 columns');
        return;
    end
    [nx,ny] = size(stack);

    for j=1 : size(p.dlocpix,1)

                xdot = p.dlocpix(j,1);
                ydot = p.dlocpix(j,2);
                
                xmin = xdot - p.cutsize*p.psf(1)/p.vox_size(1); %defining the ensemble of pixels b/w xmin:xmax ymin:ymax etc
                xmax = xdot + p.cutsize*p.psf(1)/p.vox_size(1); %these are the pixels on which the current dot is
                ymin = ydot - p.cutsize*p.psf(1)/p.vox_size(1); %gonna contribute non-negligible intensity
                ymax = ydot + p.cutsize*p.psf(1)/p.vox_size(1); %they are defined as pixels within a 3-sigma radius from the dot
                
                xp_min = max( ceil(xmin), 1);
                xp_max = min( ceil(xmax), nx );
                yp_min = max( ceil(ymin), 1);
                yp_max = min( ceil(ymax), ny );
                
                [xp,yp] = ndgrid(xp_min:xp_max,yp_min:yp_max);
                
                diffx1 =  (double(xp-1))*p.vox_size(1) - xdot*p.vox_size(1);
                diffx1 = diffx1 / ( sqrt(2.0)*p.psf(1));
                diffx2 =  (double(xp))*p.vox_size(1) - xdot*p.vox_size(1);
                diffx2 = diffx2 / ( sqrt(2.0)*p.psf(1));

                diffy1 =  (double(yp-1))*p.vox_size(2) - ydot*p.vox_size(2);
                diffy1 = diffy1 / ( sqrt(2.0)*p.psf(1) );
                diffy2 =  (double(yp))*p.vox_size(2) - ydot*p.vox_size(2);
                diffy2 = diffy2 / ( sqrt(2.0)*p.psf(1) );

                intensity = abs( erf( diffx1) - erf(diffx2) ) .* abs( erf( diffy1) - erf(diffy2) );
                intensity = p.dlocpix(j,3)*intensity;
                intensity = intensity / 4.0;

                %loop over the non-negligible intensity pixels
                %compute first the difference b/w the edges of the pixel and
                %the dot. then evaluate the integral of the gaussian PSF over the pixel.
                
                if strcmp(p.mode,'poisson')
                               %taking a poisson distribution
                   intensity = p.gain*poissrnd(intensity/p.gain);
                end
                stack2(xp_min:xp_max,yp_min:yp_max) = stack2(xp_min:xp_max,yp_min:yp_max) + intensity;
                clear('intensity');
    end
end

%% saving files
%image stack
switch p.mode
    case 'poisson'
        filestr = ['Poiss_',num2str(size(p.dlocpix,1)),'spots_bg_',num2str(p.bg_mean),'_',num2str(p.bg_sd),'_I_',num2str(p.brightness(1)),'_',num2str(p.brightness(2)),'_img0'];
    case ''
        filestr = ['Det_',  num2str(size(p.dlocpix,1)),'spots_bg_',num2str(p.bg_mean),'_',num2str(p.bg_sd),'_I_',num2str(p.brightness(1)),'_',num2str(p.brightness(2)),'_img0'];
end

imgfname = fullfile(save_dirname,[filestr,'.tif']);
img_idx=0;
while exist(imgfname,'file')
    nchar = 4 + numel(num2str(img_idx));
    imgfname = imgfname(1:(numel(imgfname)- nchar));
    img_idx = img_idx+1;
    imgfname = [imgfname,num2str(img_idx),'.tif'];   
end

save_as_tiff(stack2,imgfname);

%parameter structure
fname = imgfname(1:numel(imgfname)-4);
save([fname,'.spots'],'p','-mat');

%spot locations and intensities
loc = p.dlocpix;
save([fname,'.loc'],'loc','-ascii');


end


